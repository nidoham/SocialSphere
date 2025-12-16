package com.nidoham.socialsphere.database.cloud.helper

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlinx.coroutines.tasks.await

/**
 * Manages post reactions in Firestore.
 * Structure: /posts/{postId}/reactions/{reactionId}
 *
 * Each reaction document contains:
 * - userId: String
 * - reactionType: String (like, dislike, love, etc.)
 * - createdAt: Long (timestamp)
 */
class PostsReaction {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Toggle user's reaction on a post.
     * If reaction exists, it removes it. If not, it adds it.
     *
     * @param postId ID of the post
     * @param userId ID of the user reacting
     * @param reactionType Type of reaction (like, dislike, love, etc.)
     * @return Result indicating success or failure
     */
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: String
    ): Result<Boolean> {
        return try {
            val reactionRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")
                .document(userId)

            val snapshot = reactionRef.get().await()

            if (snapshot.exists()) {
                val existingType = snapshot.getString("reactionType")

                if (existingType == reactionType) {
                    // Same reaction → remove it
                    reactionRef.delete().await()
                    updateReactionCount(postId, reactionType, -1)
                    Result.success(false) // Reaction removed
                } else {
                    // Different reaction → update it
                    if (existingType != null) {
                        updateReactionCount(postId, existingType, -1)
                    }
                    reactionRef.update(
                        mapOf(
                            "reactionType" to reactionType,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                    updateReactionCount(postId, reactionType, 1)
                    Result.success(true) // Reaction changed
                }
            } else {
                // No reaction → add it
                reactionRef.set(
                    mapOf(
                        "userId" to userId,
                        "reactionType" to reactionType,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                updateReactionCount(postId, reactionType, 1)
                Result.success(true) // Reaction added
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a reaction to a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user reacting
     * @param reactionType Type of reaction
     * @return Result indicating success or failure
     */
    suspend fun addReaction(
        postId: String,
        userId: String,
        reactionType: String
    ): Result<Unit> {
        return try {
            val reactionRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")
                .document(userId)

            reactionRef.set(
                mapOf(
                    "userId" to userId,
                    "reactionType" to reactionType,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            updateReactionCount(postId, reactionType, 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a user's reaction from a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @return Result indicating success or failure
     */
    suspend fun removeReaction(postId: String, userId: String): Result<Unit> {
        return try {
            val reactionRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")
                .document(userId)

            val snapshot = reactionRef.get().await()
            val reactionType = snapshot.getString("reactionType")

            reactionRef.delete().await()

            if (reactionType != null) {
                updateReactionCount(postId, reactionType, -1)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific user's reaction on a post.
     *
     * @param postId ID of the post
     * @param userId ID of the user
     * @return Result with reaction type or null if no reaction
     */
    suspend fun getUserReaction(postId: String, userId: String): Result<String?> {
        return try {
            val reactionRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")
                .document(userId)

            val snapshot = reactionRef.get().await()
            val reactionType = snapshot.getString("reactionType")

            Result.success(reactionType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all reactions for a post.
     *
     * @param postId ID of the post
     * @return Result with list of reaction data
     */
    suspend fun getAllReactions(postId: String): Result<List<Map<String, Any>>> {
        return try {
            val reactionsRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")

            val snapshot = reactionsRef.get().await()
            val reactions = snapshot.documents.mapNotNull { it.data }

            Result.success(reactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get aggregated reaction counts for a post.
     *
     * @param postId ID of the post
     * @return Result with map of reaction types to counts
     */
    suspend fun getReactionCounts(postId: String): Result<Map<String, Long>> {
        return try {
            val reactionsRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")

            val snapshot = reactionsRef.get().await()
            val counts = mutableMapOf<String, Long>()

            snapshot.documents.forEach { doc ->
                val reactionType = doc.getString("reactionType")
                if (reactionType != null) {
                    counts[reactionType] = counts.getOrDefault(reactionType, 0) + 1
                }
            }

            Result.success(counts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update reaction count in the post document.
     * This keeps a denormalized count for quick access.
     *
     * @param postId ID of the post
     * @param reactionType Type of reaction
     * @param delta Change in count (+1 or -1)
     */
    private suspend fun updateReactionCount(
        postId: String,
        reactionType: String,
        delta: Long
    ) {
        try {
            val postRef = db.collection(FirebasePath.POSTS).document(postId)

            // Update the specific reaction count in the reactions map
            postRef.update(
                "reactions.$reactionType",
                FieldValue.increment(delta)
            ).await()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
            println("Failed to update reaction count: ${e.message}")
        }
    }

    /**
     * Get users who reacted with a specific reaction type.
     *
     * @param postId ID of the post
     * @param reactionType Type of reaction to filter by
     * @return Result with list of user IDs
     */
    suspend fun getUsersByReactionType(
        postId: String,
        reactionType: String
    ): Result<List<String>> {
        return try {
            val reactionsRef = db.collection(FirebasePath.POSTS)
                .document(postId)
                .collection("reactions")
                .whereEqualTo("reactionType", reactionType)

            val snapshot = reactionsRef.get().await()
            val userIds = snapshot.documents.mapNotNull { it.getString("userId") }

            Result.success(userIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}