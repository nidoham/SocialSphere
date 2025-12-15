package com.nidoham.socialsphere.posts.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.nidoham.socialsphere.posts.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class PostsHelpers private constructor(
    private val firestore: FirebaseFirestore
) {
    // <CHANGE> Lazy initialization - collection reference created only when first accessed
    private val postsCollection by lazy { firestore.collection("posts") }

    companion object {
        // <CHANGE> Thread-safe singleton with double-checked locking
        @Volatile
        private var instance: PostsHelpers? = null

        fun getInstance(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()): PostsHelpers {
            return instance ?: synchronized(this) {
                instance ?: PostsHelpers(firestore).also {
                    instance = it
                    it.initializeSettings()
                }
            }
        }

        // <CHANGE> Pre-allocated constant maps to reduce object allocation
        private val SOFT_DELETE_BASE = mapOf(
            "deleted" to true,
            "hidden" to true
        )

        private val VISIBLE_FILTERS = mapOf(
            "deleted" to false,
            "hidden" to false
        )
    }

    private fun initializeSettings() {
        try {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(100 * 1024 * 1024L)
                .build()
        } catch (_: Exception) { /* Already configured */ }
    }

    /* -------------------- CREATE -------------------- */

    suspend fun createPost(post: Post): Result<String> = withIOContext {
        val docRef = postsCollection.document()
        docRef.set(post.copy(postId = docRef.id)).await()
        docRef.id
    }

    // <CHANGE> Batch create for multiple posts
    suspend fun createPosts(posts: List<Post>): Result<List<String>> = withIOContext {
        require(posts.size <= 500) { "Batch limit is 500" }
        val batch = firestore.batch()
        val ids = posts.map { post ->
            val docRef = postsCollection.document()
            batch.set(docRef, post.copy(postId = docRef.id))
            docRef.id
        }
        batch.commit().await()
        ids
    }

    /* -------------------- FETCH -------------------- */

    suspend fun getPost(
        postId: String,
        source: Source = Source.DEFAULT
    ): Result<Post?> = withIOContext {
        postsCollection.document(postId)
            .get(source)
            .await()
            .toObject(Post::class.java)
    }

    // <CHANGE> Unified fetch method with builder pattern
    suspend fun fetchPosts(
        userId: String? = null,
        limit: Long = 20,
        lastTimestamp: Long? = null,
        source: Source = Source.DEFAULT
    ): Result<List<Post>> = withIOContext {
        buildVisiblePostsQuery()
            .let { q -> userId?.let { q.whereEqualTo("userId", it) } ?: q }
            .let { q -> lastTimestamp?.let { q.whereLessThan("timestamp", it) } ?: q }
            .limit(limit)
            .get(source)
            .await()
            .toObjects(Post::class.java)
    }

    // <CHANGE> Extracted common query builder
    private fun buildVisiblePostsQuery(): Query = postsCollection
        .whereEqualTo("deleted", false)
        .whereEqualTo("hidden", false)
        .orderBy("timestamp", Query.Direction.DESCENDING)

    /* -------------------- UPDATE -------------------- */

    suspend fun updateFields(
        postId: String,
        fields: Map<String, Any?>
    ): Result<Unit> = withIOContext {
        requireValidId(postId)
        require(fields.isNotEmpty()) { "Fields cannot be empty" }
        postsCollection.document(postId).update(fields).await()
    }

    // <CHANGE> Atomic reaction updates using FieldValue.increment
    suspend fun adjustReaction(
        postId: String,
        reactionType: String,
        delta: Long = 1
    ): Result<Unit> = withIOContext {
        requireValidId(postId)
        postsCollection.document(postId)
            .update("reactions.$reactionType", FieldValue.increment(delta))
            .await()
    }

    // <CHANGE> Convenience methods using adjustReaction
    suspend fun incrementReaction(postId: String, type: String) = adjustReaction(postId, type, 1)
    suspend fun decrementReaction(postId: String, type: String) = adjustReaction(postId, type, -1)

    // <CHANGE> Atomic counter updates
    suspend fun adjustCounter(
        postId: String,
        field: String,
        delta: Long = 1
    ): Result<Unit> = withIOContext {
        requireValidId(postId)
        postsCollection.document(postId)
            .update(field, FieldValue.increment(delta))
            .await()
    }

    suspend fun batchUpdate(
        updates: List<Pair<String, Map<String, Any?>>>
    ): Result<Unit> = withIOContext {
        require(updates.size <= 500) { "Batch limit is 500" }
        firestore.batch().apply {
            updates.forEach { (id, fields) -> update(postsCollection.document(id), fields) }
        }.commit().await()
    }

    /* -------------------- DELETE -------------------- */

    suspend fun deletePost(postId: String): Result<Unit> = withIOContext {
        requireValidId(postId)
        postsCollection.document(postId).delete().await()
    }

    suspend fun softDeletePost(postId: String): Result<Unit> = withIOContext {
        requireValidId(postId)
        postsCollection.document(postId)
            .update(SOFT_DELETE_BASE + ("deletedTimestamp" to System.currentTimeMillis()))
            .await()
    }

    suspend fun batchSoftDelete(postIds: List<String>): Result<Unit> = withIOContext {
        require(postIds.size <= 500) { "Batch limit is 500" }
        val timestamp = System.currentTimeMillis()
        val deleteData = SOFT_DELETE_BASE + ("deletedTimestamp" to timestamp)

        firestore.batch().apply {
            postIds.forEach { update(postsCollection.document(it), deleteData) }
        }.commit().await()
    }

    /* -------------------- HELPERS -------------------- */

    // <CHANGE> Inline validation for zero overhead
    @Suppress("NOTHING_TO_INLINE")
    private inline fun requireValidId(postId: String) {
        require(postId.isNotBlank()) { "PostId cannot be empty" }
    }

    // <CHANGE> Centralized IO context wrapper with Result
    private suspend inline fun <T> withIOContext(
        crossinline block: suspend () -> T
    ): Result<T> = runCatching {
        withContext(Dispatchers.IO) { block() }
    }

    /* -------------------- NETWORK -------------------- */

    fun enableNetwork() { firestore.enableNetwork() }
    fun disableNetwork() { firestore.disableNetwork() }
    suspend fun clearCache(): Result<Unit> = withIOContext { firestore.clearPersistence().await() }
}