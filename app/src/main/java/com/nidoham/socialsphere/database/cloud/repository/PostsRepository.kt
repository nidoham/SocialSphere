package com.nidoham.socialsphere.database.cloud.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nidoham.socialsphere.database.cloud.helper.PostsReaction
import com.nidoham.socialsphere.database.cloud.model.Post
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing posts in Firestore.
 * Handles CRUD operations and pagination for posts.
 */
class PostsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection(FirebasePath.POSTS)
    private val reactionHelper = PostsReaction()

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Create a new post.
     *
     * @param post Post object to create
     * @return Result with the created post ID
     */
    suspend fun createPost(post: Post): Result<String> {
        return try {
            val postId = if (post.id.isEmpty()) {
                postsCollection.document().id
            } else {
                post.id
            }

            val postWithId = post.copy(
                id = postId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            postsCollection.document(postId).set(postWithId).await()
            Result.success(postId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing post.
     *
     * @param postId ID of the post to update
     * @param updates Map of field names to new values
     * @return Result indicating success or failure
     */
    suspend fun updatePost(postId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updatesWithTimestamp = updates.toMutableMap().apply {
                put("updatedAt", System.currentTimeMillis())
                if (updates.keys.any { it != "updatedAt" }) {
                    put("isEdited", true)
                }
            }

            postsCollection.document(postId).update(updatesWithTimestamp).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a post (soft delete).
     *
     * @param postId ID of the post to delete
     * @return Result indicating success or failure
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "deletedAt" to System.currentTimeMillis(),
                "status" to "DELETED"
            )
            postsCollection.document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently delete a post from Firestore.
     *
     * @param postId ID of the post to permanently delete
     * @return Result indicating success or failure
     */
    suspend fun permanentlyDeletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single post by ID.
     *
     * @param postId ID of the post
     * @return Result with Post object or null if not found
     */
    suspend fun getPost(postId: String): Result<Post?> {
        return try {
            val snapshot = postsCollection.document(postId).get().await()
            val post = snapshot.toObject(Post::class.java)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get posts with pagination (infinite scroll).
     *
     * @param pageSize Number of posts per page
     * @param lastDocument Last document from previous page (null for first page)
     * @param orderBy Field to order by (default: createdAt)
     * @param descending Sort direction (default: true for newest first)
     * @return Result with PaginatedResult containing posts and last document
     */
    suspend fun getPosts(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null,
        orderBy: String = "createdAt",
        descending: Boolean = true
    ): Result<PaginatedResult<Post>> {
        return try {
            var query: Query = postsCollection
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deletedAt", 0L)

            query = if (descending) {
                query.orderBy(orderBy, Query.Direction.DESCENDING)
            } else {
                query.orderBy(orderBy, Query.Direction.ASCENDING)
            }

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            query = query.limit(pageSize.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.documents.size == pageSize

            Result.success(
                PaginatedResult(
                    data = posts,
                    lastDocument = lastDoc,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get posts by a specific user.
     *
     * @param userId ID of the user
     * @param pageSize Number of posts per page
     * @param lastDocument Last document from previous page
     * @return Result with PaginatedResult
     */
    suspend fun getPostsByUser(
        userId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Post>> {
        return try {
            var query: Query = postsCollection
                .whereEqualTo("authorId", userId)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deletedAt", 0L)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            query = query.limit(pageSize.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.documents.size == pageSize

            Result.success(
                PaginatedResult(
                    data = posts,
                    lastDocument = lastDoc,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get posts by hashtag.
     *
     * @param hashtag Hashtag to search for
     * @param pageSize Number of posts per page
     * @param lastDocument Last document from previous page
     * @return Result with PaginatedResult
     */
    suspend fun getPostsByHashtag(
        hashtag: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Post>> {
        return try {
            var query: Query = postsCollection
                .whereArrayContains("hashtags", hashtag)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deletedAt", 0L)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            query = query.limit(pageSize.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.documents.size == pageSize

            Result.success(
                PaginatedResult(
                    data = posts,
                    lastDocument = lastDoc,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get posts from a specific page.
     *
     * @param pageId ID of the page
     * @param pageSize Number of posts per page
     * @param lastDocument Last document from previous page
     * @return Result with PaginatedResult
     */
    suspend fun getPostsByPage(
        pageId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Post>> {
        return try {
            var query: Query = postsCollection
                .whereEqualTo("parentPageId", pageId)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deletedAt", 0L)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            query = query.limit(pageSize.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.documents.size == pageSize

            Result.success(
                PaginatedResult(
                    data = posts,
                    lastDocument = lastDoc,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get posts from a specific group.
     *
     * @param groupId ID of the group
     * @param pageSize Number of posts per page
     * @param lastDocument Last document from previous page
     * @return Result with PaginatedResult
     */
    suspend fun getPostsByGroup(
        groupId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Post>> {
        return try {
            var query: Query = postsCollection
                .whereEqualTo("parentGroupId", groupId)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deletedAt", 0L)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            query = query.limit(pageSize.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.documents.size == pageSize

            Result.success(
                PaginatedResult(
                    data = posts,
                    lastDocument = lastDoc,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============ Reaction Methods ============

    /**
     * Toggle user's reaction on a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @param reactionType Type of reaction
     * @return Result with boolean indicating if reaction is active
     */
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: String
    ): Result<Boolean> {
        return reactionHelper.toggleReaction(postId, userId, reactionType)
    }

    /**
     * Add a reaction to a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @param reactionType Type of reaction
     * @return Result indicating success or failure
     */
    suspend fun addReaction(
        postId: String,
        userId: String,
        reactionType: String
    ): Result<Unit> {
        return reactionHelper.addReaction(postId, userId, reactionType)
    }

    /**
     * Remove a reaction from a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @return Result indicating success or failure
     */
    suspend fun removeReaction(postId: String, userId: String): Result<Unit> {
        return reactionHelper.removeReaction(postId, userId)
    }

    /**
     * Get user's reaction on a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @return Result with reaction type or null
     */
    suspend fun getUserReaction(postId: String, userId: String): Result<String?> {
        return reactionHelper.getUserReaction(postId, userId)
    }

    /**
     * Get all reaction counts for a post.
     *
     * @param postId ID of the post
     * @return Result with map of reaction types to counts
     */
    suspend fun getReactionCounts(postId: String): Result<Map<String, Long>> {
        return reactionHelper.getReactionCounts(postId)
    }

    // ============ Increment Methods ============

    /**
     * Increment comment count.
     *
     * @param postId ID of the post
     * @param increment Amount to increment (default: 1)
     * @return Result indicating success or failure
     */
    suspend fun incrementCommentCount(postId: String, increment: Long = 1): Result<Unit> {
        return try {
            val updates = mapOf(
                "commentCount" to com.google.firebase.firestore.FieldValue.increment(increment)
            )
            postsCollection.document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increment share count.
     *
     * @param postId ID of the post
     * @param increment Amount to increment (default: 1)
     * @return Result indicating success or failure
     */
    suspend fun incrementShareCount(postId: String, increment: Long = 1): Result<Unit> {
        return try {
            val updates = mapOf(
                "shareCount" to com.google.firebase.firestore.FieldValue.increment(increment)
            )
            postsCollection.document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increment engagement score.
     *
     * @param postId ID of the post
     * @param increment Amount to increment (default: 1)
     * @return Result indicating success or failure
     */
    suspend fun incrementEngagementScore(postId: String, increment: Long = 1): Result<Unit> {
        return try {
            val updates = mapOf(
                "engagementScore" to com.google.firebase.firestore.FieldValue.increment(increment)
            )
            postsCollection.document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class representing paginated results.
 *
 * @property data List of items in current page
 * @property lastDocument Last document snapshot for pagination
 * @property hasMore Whether there are more items to load
 */
data class PaginatedResult<T>(
    val data: List<T>,
    val lastDocument: DocumentSnapshot?,
    val hasMore: Boolean
)