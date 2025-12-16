package com.nidoham.socialsphere.database.cloud.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nidoham.socialsphere.database.cloud.model.Reaction
import com.nidoham.socialsphere.database.cloud.model.Story
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlinx.coroutines.tasks.await

/**
 * StoryRepository - Production-Ready Story Management
 */
class StoryRepository private constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "StoryRepository"
        private const val COLLECTION_REACTIONS = "reactions"
        private const val DOCUMENT_COUNTS = "counts"

        @Volatile
        private var INSTANCE: StoryRepository? = null

        fun getInstance(
            firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
        ): StoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoryRepository(firestore).also { INSTANCE = it }
            }
        }
    }

    private val storiesCollection = firestore.collection(FirebasePath.STORIES)

    // ==================== CREATE ====================

    /**
     * Creates a new story
     * @param story Story object to create
     * @return Result containing the story ID or error
     */
    suspend fun createStory(story: Story): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val expiresAt = Story.getExpiryTimestamp(now)

            val storyToCreate = story.copy(
                id = "", // Will be set by Firestore
                createdAt = now,
                expiresAt = expiresAt,
                reactions = Reaction(), // Empty reactions
                viewCount = 0L,
                replyCount = 0L,
                shareCount = 0L
            )

            val docRef = storiesCollection.add(storyToCreate).await()
            Log.d(TAG, "Story created successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating story", e)
            Result.failure(Exception("Failed to create story: ${e.message}", e))
        }
    }

    // ==================== READ ====================

    /**
     * Fetches recent active stories (within 24 hours)
     * @param limit Maximum number of stories to fetch
     * @return Result containing list of stories
     */
    suspend fun getRecentStories(limit: Int = 20): Result<List<Story>> {
        return try {
            val now = System.currentTimeMillis()
            val twentyFourHoursAgo = now - (Story.DEFAULT_EXPIRY_HOURS * 60 * 60 * 1000)

            val snapshot = storiesCollection
                .whereGreaterThan("createdAt", twentyFourHoursAgo)
                .whereEqualTo("isBanned", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Fetched ${stories.size} recent stories")
            Result.success(stories)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent stories", e)
            Result.failure(Exception("Failed to fetch stories: ${e.message}", e))
        }
    }

    /**
     * Fetches stories by a specific user
     * @param userId User ID
     * @param limit Maximum number of stories to fetch
     * @return Result containing list of user's stories
     */
    suspend fun getUserStories(userId: String, limit: Int = 20): Result<List<Story>> {
        return try {
            val snapshot = storiesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Fetched ${stories.size} stories for user: $userId")
            Result.success(stories)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user stories", e)
            Result.failure(Exception("Failed to fetch user stories: ${e.message}", e))
        }
    }

    /**
     * Gets a single story by ID
     * @param storyId Story ID
     * @return Result containing the story or null
     */
    suspend fun getStoryById(storyId: String): Result<Story?> {
        return try {
            val snapshot = storiesCollection.document(storyId).get().await()
            val story = snapshot.toObject(Story::class.java)?.copy(id = snapshot.id)
            Result.success(story)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching story by ID", e)
            Result.failure(Exception("Failed to fetch story: ${e.message}", e))
        }
    }

    // ==================== UPDATE ====================

    /**
     * Increments view count for a story
     * @param storyId Story ID
     * @return Result indicating success or failure
     */
    suspend fun incrementViewCount(storyId: String): Result<Unit> {
        return try {
            storiesCollection.document(storyId)
                .update("viewCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing view count", e)
            Result.failure(Exception("Failed to increment views: ${e.message}", e))
        }
    }

    /**
     * Increments reply count for a story
     * @param storyId Story ID
     * @return Result indicating success or failure
     */
    suspend fun incrementReplyCount(storyId: String): Result<Unit> {
        return try {
            storiesCollection.document(storyId)
                .update("replyCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing reply count", e)
            Result.failure(Exception("Failed to increment replies: ${e.message}", e))
        }
    }

    /**
     * Increments share count for a story
     * @param storyId Story ID
     * @return Result indicating success or failure
     */
    suspend fun incrementShareCount(storyId: String): Result<Unit> {
        return try {
            storiesCollection.document(storyId)
                .update("shareCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing share count", e)
            Result.failure(Exception("Failed to increment shares: ${e.message}", e))
        }
    }

    /**
     * Batch increments view count for multiple stories
     * @param storyIds List of story IDs
     * @return Result indicating success or failure
     */
    suspend fun batchIncrementViews(storyIds: List<String>): Result<Unit> {
        if (storyIds.isEmpty()) return Result.success(Unit)

        return try {
            val batch = firestore.batch()
            storyIds.forEach { storyId ->
                val storyRef = storiesCollection.document(storyId)
                batch.update(storyRef, "viewCount", FieldValue.increment(1))
            }
            batch.commit().await()
            Log.d(TAG, "Batch updated ${storyIds.size} story views")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch incrementing views", e)
            Result.failure(Exception("Failed to batch update views: ${e.message}", e))
        }
    }

    // ==================== REACTIONS ====================

    /**
     * Toggles reaction (like/dislike) on a story
     * @param storyId Story ID
     * @param userId User ID
     * @param isLike True for like, false for dislike
     * @return Result indicating success or failure
     */
    suspend fun toggleReaction(storyId: String, userId: String, isLike: Boolean = true): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val storyRef = storiesCollection.document(storyId)
                val reactionsRef = storyRef.collection(COLLECTION_REACTIONS).document(DOCUMENT_COUNTS)
                val userReactionRef = storyRef.collection(COLLECTION_REACTIONS).document(userId)

                val storySnapshot = transaction.get(storyRef)
                if (!storySnapshot.exists()) {
                    throw IllegalArgumentException("Story not found")
                }

                val userReactionSnapshot = transaction.get(userReactionRef)
                val reactionField = if (isLike) "likes" else "dislikes"
                val currentUserReaction = userReactionSnapshot.getBoolean(reactionField) ?: false

                if (currentUserReaction) {
                    // Remove reaction
                    transaction.update(userReactionRef, reactionField, false)
                    transaction.update(reactionsRef, reactionField, FieldValue.increment(-1))

                    // Update story document
                    transaction.update(storyRef, "reactions.$reactionField", FieldValue.increment(-1))
                } else {
                    // Add reaction
                    val userReactionData = mapOf(reactionField to true)
                    transaction.set(userReactionRef, userReactionData, SetOptions.merge())
                    transaction.update(reactionsRef, reactionField, FieldValue.increment(1))

                    // Update story document
                    transaction.update(storyRef, "reactions.$reactionField", FieldValue.increment(1))
                }
            }.await()

            Log.d(TAG, "Reaction toggled for story: $storyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling reaction", e)
            Result.failure(Exception("Failed to toggle reaction: ${e.message}", e))
        }
    }

    /**
     * Gets reaction counts for a story
     * @param storyId Story ID
     * @return Result containing Reaction object
     */
    suspend fun getReactionCounts(storyId: String): Result<Reaction> {
        return try {
            val reactionsRef = storiesCollection
                .document(storyId)
                .collection(COLLECTION_REACTIONS)
                .document(DOCUMENT_COUNTS)

            val snapshot = reactionsRef.get().await()
            val likes = snapshot.getLong("likes") ?: 0L
            val dislikes = snapshot.getLong("dislikes") ?: 0L

            Result.success(Reaction(likes, dislikes))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reaction counts", e)
            Result.success(Reaction()) // Return empty reaction on error
        }
    }

    /**
     * Gets user's reaction status for a story
     * @param storyId Story ID
     * @param userId User ID
     * @return Result containing map of reaction status
     */
    suspend fun getUserReaction(storyId: String, userId: String): Result<Map<String, Boolean>> {
        return try {
            val userReactionRef = storiesCollection
                .document(storyId)
                .collection(COLLECTION_REACTIONS)
                .document(userId)

            val snapshot = userReactionRef.get().await()
            val reactions = mapOf(
                "likes" to (snapshot.getBoolean("likes") ?: false),
                "dislikes" to (snapshot.getBoolean("dislikes") ?: false)
            )

            Result.success(reactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user reaction", e)
            Result.success(mapOf("likes" to false, "dislikes" to false))
        }
    }

    /**
     * Checks if user has liked a story
     * @param storyId Story ID
     * @param userId User ID
     * @return True if user has liked the story
     */
    suspend fun hasUserLiked(storyId: String, userId: String): Boolean {
        return try {
            val userReactionRef = storiesCollection
                .document(storyId)
                .collection(COLLECTION_REACTIONS)
                .document(userId)

            val snapshot = userReactionRef.get().await()
            snapshot.getBoolean("likes") ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user like status", e)
            false
        }
    }

    // ==================== DELETE ====================

    /**
     * Deletes a story
     * @param storyId Story ID
     * @return Result indicating success or failure
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            // Delete reactions subcollection first (optional, can use Cloud Function)
            val reactionsSnapshot = storiesCollection
                .document(storyId)
                .collection(COLLECTION_REACTIONS)
                .get()
                .await()

            val batch = firestore.batch()
            reactionsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            // Delete the story document
            storiesCollection.document(storyId).delete().await()

            Log.d(TAG, "Story deleted: $storyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting story", e)
            Result.failure(Exception("Failed to delete story: ${e.message}", e))
        }
    }

    /**
     * Soft deletes a story (bans it)
     * @param storyId Story ID
     * @return Result indicating success or failure
     */
    suspend fun banStory(storyId: String): Result<Unit> {
        return try {
            storiesCollection.document(storyId)
                .update("isBanned", true)
                .await()
            Log.d(TAG, "Story banned: $storyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error banning story", e)
            Result.failure(Exception("Failed to ban story: ${e.message}", e))
        }
    }

    // ==================== CLEANUP ====================

    /**
     * Deletes expired stories (older than 24 hours)
     * Should be called periodically or via Cloud Function
     * @return Result containing number of deleted stories
     */
    suspend fun deleteExpiredStories(): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val snapshot = storiesCollection
                .whereLessThan("expiresAt", now)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            val count = snapshot.documents.size
            Log.d(TAG, "Deleted $count expired stories")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expired stories", e)
            Result.failure(Exception("Failed to delete expired stories: ${e.message}", e))
        }
    }
}