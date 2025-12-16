package com.nidoham.socialsphere.database.cloud.helper

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlinx.coroutines.tasks.await

/**
 * Simple reaction counter: /stories/{storiesId}/reactions
 * Structure: { likes: 5, dislikes: 2, loves: 3 }
 */
class StoriesReaction {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Toggle reaction count: /stories/{storiesId}/reactions
     * First click: +1, Second click: -1 (never goes below 0)
     */
    suspend fun toggleReaction(storiesId: String, reactionType: String): Result<Unit> {
        return try {
            val reactionsRef = db.collection(FirebasePath.STORIES)
                .document(storiesId)
                .collection("reactions")
                .document("counts")

            // Check current count
            val snapshot = reactionsRef.get().await()
            val currentCount = snapshot.getLong(reactionType) ?: 0L

            return if (currentCount > 0) {
                // Has reaction → decrement
                reactionsRef.update(reactionType, FieldValue.increment(-1)).await()
                Result.success(Unit)
            } else {
                // No reaction → increment
                reactionsRef.update(reactionType, FieldValue.increment(1)).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct increment (for admin/sync)
     */
    suspend fun incrementReaction(storiesId: String, reactionType: String): Result<Unit> {
        return try {
            val reactionsRef = db.collection(FirebasePath.STORIES)
                .document(storiesId)
                .collection("reactions")
                .document("counts")

            reactionsRef.update(reactionType, FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all reaction counts
     */
    suspend fun getReactionCounts(storiesId: String): Result<Map<String, Long>> {
        return try {
            val reactionsRef = db.collection(FirebasePath.STORIES)
                .document(storiesId)
                .collection("reactions")
                .document("counts")

            val snapshot = reactionsRef.get().await()
            val counts = snapshot.data?.mapValues { (_, value) ->
                value as? Long ?: 0L
            } ?: emptyMap()

            Result.success(counts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
