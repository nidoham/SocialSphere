package com.nidoham.socialsphere.extractor

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Data class representing reaction counts for a piece of content.
 * Stored at: /reactions/{contentType}/{contentType}s/{contentId}
 */
data class ReactionCounts(
    val likes: Long = 0L,
    val loves: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun empty() = ReactionCounts(likes = 0L, loves = 0L)
    }

    fun toMap(): Map<String, Any> = mapOf(
        "likes" to likes,
        "loves" to loves,
        "timestamp" to timestamp
    )
}

/**
 * Data class representing a user's reaction to content.
 * Stored at: /activities/reactions/{contentId}/{userId}
 */
data class UserReaction(
    val contentType: String = "",
    val contentId: String = "",
    val userId: String = "",
    val reactionType: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "contentType" to contentType,
        "contentId" to contentId,
        "userId" to userId,
        "reactionType" to reactionType,
        "timestamp" to timestamp
    )
}

/**
 * Sealed hierarchy for content types with type-safe access.
 */
sealed class ContentType(val value: String) {
    data object Post : ContentType("post")
    data object Comment : ContentType("comment")
    data object Story : ContentType("story")

    companion object {
        fun fromString(value: String): ContentType? = when (value) {
            "post" -> Post
            "comment" -> Comment
            "story" -> Story
            else -> null
        }
    }
}

/**
 * Sealed hierarchy for reaction types with type-safe access.
 */
sealed class ReactionType(val value: String) {
    data object Like : ReactionType("likes")
    data object Love : ReactionType("loves")

    companion object {
        fun fromString(value: String): ReactionType? = when (value) {
            "likes" -> Like
            "loves" -> Love
            else -> null
        }
    }
}

/**
 * Custom exceptions for reaction operations.
 */
sealed class ReactionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidInput(message: String) : ReactionException(message)
    class TransactionFailed(message: String, cause: Throwable? = null) : ReactionException(message, cause)
    class DocumentNotFound(message: String) : ReactionException(message)
    class NetworkError(message: String, cause: Throwable) : ReactionException(message, cause)
}

/**
 * Professional reaction management system for Firebase Realtime Database.
 *
 * This class handles all reaction-related operations including:
 * - Toggling reactions (like/love)
 * - Retrieving reaction counts
 * - Managing user-specific reactions
 * - Batch operations for multiple content items
 *
 * All operations are atomic and use Firebase transactions to prevent race conditions.
 * The class is designed to be thread-safe and can be used as a singleton.
 *
 * @property db Firebase Realtime Database instance
 */
@Singleton
class ReactionManager @Inject constructor(
    private val db: FirebaseDatabase
) {
    companion object {
        private const val TAG = "ReactionManager"

        // Database paths
        private const val PATH_REACTIONS = "reactions"
        private const val PATH_ACTIVITIES = "activities"
        private const val PATH_REACTIONS_ACTIVITY = "reactions"

        // Field names
        private const val FIELD_REACTION_TYPE = "reactionType"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_LIKES = "likes"
        private const val FIELD_LOVES = "loves"

        // Validation constants
        private const val MAX_CONTENT_ID_LENGTH = 100
        private const val MAX_USER_ID_LENGTH = 100
        private const val BATCH_CHUNK_SIZE = 10
    }

    /**
     * Gets reference to the reaction counts node.
     * Path: /reactions/{contentType}/{contentType}s/{contentId}
     */
    private fun getCountsRef(contentType: String, contentId: String): DatabaseReference {
        return db.reference
            .child(PATH_REACTIONS)
            .child(contentType)
            .child("${contentType}s")
            .child(contentId)
    }

    /**
     * Gets reference to the user's reaction node.
     * Path: /activities/reactions/{contentId}/{userId}
     */
    private fun getUserReactionRef(contentId: String, userId: String): DatabaseReference {
        return db.reference
            .child(PATH_ACTIVITIES)
            .child(PATH_REACTIONS_ACTIVITY)
            .child(contentId)
            .child(userId)
    }

    /**
     * Validates input parameters for reaction operations.
     *
     * @throws ReactionException.InvalidInput if validation fails
     */
    private fun validateInput(contentId: String, userId: String) {
        require(contentId.isNotBlank()) {
            "Content ID cannot be blank"
        }
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }
        require(contentId.length <= MAX_CONTENT_ID_LENGTH) {
            "Content ID exceeds maximum length of $MAX_CONTENT_ID_LENGTH"
        }
        require(userId.length <= MAX_USER_ID_LENGTH) {
            "User ID exceeds maximum length of $MAX_USER_ID_LENGTH"
        }
    }

    /**
     * Helper extension to convert DataSnapshot to ReactionCounts
     */
    private fun DataSnapshot.toReactionCounts(): ReactionCounts {
        return ReactionCounts(
            likes = child(FIELD_LIKES).getValue(Long::class.java) ?: 0L,
            loves = child(FIELD_LOVES).getValue(Long::class.java) ?: 0L,
            timestamp = child(FIELD_TIMESTAMP).getValue(Long::class.java) ?: System.currentTimeMillis()
        )
    }

    /**
     * Suspending wrapper for Firebase Realtime Database transactions
     */
    private suspend fun DatabaseReference.runTransactionSuspend(
        handler: (MutableData) -> Transaction.Result
    ): DataSnapshot = suspendCancellableCoroutine { continuation ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                return handler(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    continuation.resumeWithException(
                        ReactionException.TransactionFailed(
                            "Transaction failed: ${error.message}",
                            error.toException()
                        )
                    )
                } else if (currentData != null) {
                    continuation.resume(currentData)
                } else {
                    continuation.resumeWithException(
                        ReactionException.TransactionFailed("Transaction completed but snapshot is null")
                    )
                }
            }
        })
    }

    /**
     * Toggles a reaction for a user on a piece of content.
     *
     * This operation is atomic and handles three scenarios:
     * 1. Removing an existing reaction (toggle off)
     * 2. Changing from one reaction type to another
     * 3. Adding a new reaction
     *
     * The operation uses Firebase transactions to ensure data consistency
     * and prevent race conditions when multiple users react simultaneously.
     *
     * @param contentType Type of content being reacted to
     * @param contentId Unique identifier for the content
     * @param userId Unique identifier for the user
     * @param reaction Type of reaction to toggle
     * @return Result containing updated reaction counts or error
     *
     * @throws ReactionException.InvalidInput if parameters are invalid
     * @throws ReactionException.TransactionFailed if the transaction fails
     */
    suspend fun toggleReaction(
        contentType: ContentType,
        contentId: String,
        userId: String,
        reaction: ReactionType
    ): Result<ReactionCounts> = runCatching {
        validateInput(contentId, userId)

        Log.d(TAG, "Toggling ${reaction.value} for user=$userId on $contentType=$contentId")

        try {
            // First, check the current user reaction
            val userRef = getUserReactionRef(contentId, userId)
            val userSnapshot = userRef.get().await()
            val currentReaction = userSnapshot.child(FIELD_REACTION_TYPE).getValue(String::class.java)
            val isTogglingOff = currentReaction == reaction.value

            // Perform transaction on counts
            val countsRef = getCountsRef(contentType.value, contentId)
            val resultSnapshot = countsRef.runTransactionSuspend { currentData ->
                val currentCounts = if (currentData.value != null) {
                    ReactionCounts(
                        likes = currentData.child(FIELD_LIKES).getValue(Long::class.java) ?: 0L,
                        loves = currentData.child(FIELD_LOVES).getValue(Long::class.java) ?: 0L,
                        timestamp = currentData.child(FIELD_TIMESTAMP).getValue(Long::class.java)
                            ?: System.currentTimeMillis()
                    )
                } else {
                    ReactionCounts.empty()
                }

                val updatedCounts = when {
                    // Case 1: User is removing their reaction
                    isTogglingOff -> {
                        when (reaction) {
                            ReactionType.Like -> currentCounts.copy(
                                likes = maxOf(0, currentCounts.likes - 1)
                            )
                            ReactionType.Love -> currentCounts.copy(
                                loves = maxOf(0, currentCounts.loves - 1)
                            )
                        }
                    }

                    // Case 2: User is changing their reaction type
                    currentReaction != null -> {
                        val decremented = when (ReactionType.fromString(currentReaction)) {
                            ReactionType.Like -> currentCounts.copy(
                                likes = maxOf(0, currentCounts.likes - 1)
                            )
                            ReactionType.Love -> currentCounts.copy(
                                loves = maxOf(0, currentCounts.loves - 1)
                            )
                            null -> currentCounts
                        }
                        when (reaction) {
                            ReactionType.Like -> decremented.copy(likes = decremented.likes + 1)
                            ReactionType.Love -> decremented.copy(loves = decremented.loves + 1)
                        }
                    }

                    // Case 3: User is adding a new reaction
                    else -> {
                        when (reaction) {
                            ReactionType.Like -> currentCounts.copy(likes = currentCounts.likes + 1)
                            ReactionType.Love -> currentCounts.copy(loves = currentCounts.loves + 1)
                        }
                    }
                }

                currentData.value = updatedCounts.toMap()
                Transaction.success(currentData)
            }

            // Update user reaction document
            if (isTogglingOff) {
                Log.d(TAG, "Removing reaction: ${reaction.value}")
                userRef.removeValue().await()
            } else {
                Log.d(TAG, if (currentReaction != null) {
                    "Changing reaction from $currentReaction to ${reaction.value}"
                } else {
                    "Adding new reaction: ${reaction.value}"
                })
                userRef.setValue(
                    UserReaction(
                        contentType = contentType.value,
                        contentId = contentId,
                        userId = userId,
                        reactionType = reaction.value
                    ).toMap()
                ).await()
            }

            // Return the updated counts
            resultSnapshot.toReactionCounts()

        } catch (e: DatabaseException) {
            Log.e(TAG, "Database error during toggleReaction", e)
            throw ReactionException.TransactionFailed(
                "Failed to toggle reaction: ${e.message}",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during toggleReaction", e)
            throw ReactionException.TransactionFailed(
                "Unexpected error toggling reaction: ${e.message}",
                e
            )
        }
    }.onFailure { error ->
        Log.e(TAG, "toggleReaction failed", error)
    }

    /**
     * Gets the current reaction counts for a piece of content.
     *
     * @param contentType Type of content
     * @param contentId Unique identifier for the content
     * @return Result containing reaction counts or default values if not found
     *
     * @throws ReactionException.InvalidInput if parameters are invalid
     */
    suspend fun getReactionCounts(
        contentType: ContentType,
        contentId: String
    ): Result<ReactionCounts> = runCatching {
        require(contentId.isNotBlank()) { "Content ID cannot be blank" }

        val snapshot = getCountsRef(contentType.value, contentId).get().await()

        if (!snapshot.exists()) {
            Log.d(TAG, "No reaction counts found for $contentType=$contentId")
            ReactionCounts.empty()
        } else {
            snapshot.toReactionCounts()
        }
    }.onFailure { error ->
        Log.e(TAG, "Failed to get reaction counts for $contentType=$contentId", error)
    }

    /**
     * Gets the user's current reaction to a piece of content.
     *
     * @param contentId Unique identifier for the content
     * @param userId Unique identifier for the user
     * @return Result containing the user's reaction type, or null if no reaction exists
     *
     * @throws ReactionException.InvalidInput if parameters are invalid
     */
    suspend fun getUserReaction(
        contentId: String,
        userId: String
    ): Result<ReactionType?> = runCatching {
        validateInput(contentId, userId)

        val snapshot = getUserReactionRef(contentId, userId).get().await()

        if (!snapshot.exists()) {
            null
        } else {
            val reactionValue = snapshot.child(FIELD_REACTION_TYPE).getValue(String::class.java)
            ReactionType.fromString(reactionValue ?: "")
        }
    }.onFailure { error ->
        Log.e(TAG, "Failed to get user reaction for content=$contentId, user=$userId", error)
    }

    /**
     * Initializes reaction counts node for new content.
     *
     * This operation is idempotent - it won't overwrite existing data.
     * Should be called when creating new content to ensure the counts node exists.
     *
     * @param contentType Type of content
     * @param contentId Unique identifier for the content
     * @return Result indicating success or failure
     */
    suspend fun initializeReactionCounts(
        contentType: ContentType,
        contentId: String
    ): Result<Unit> = runCatching {
        require(contentId.isNotBlank()) { "Content ID cannot be blank" }

        val countsRef = getCountsRef(contentType.value, contentId)

        // Check if it exists first
        val snapshot = countsRef.get().await()
        if (!snapshot.exists()) {
            countsRef.setValue(ReactionCounts.empty().toMap()).await()
            Log.d(TAG, "Initialized reaction counts for $contentType=$contentId")
        } else {
            Log.d(TAG, "Reaction counts already exist for $contentType=$contentId")
        }

        Unit
    }.onFailure { error ->
        Log.e(TAG, "Failed to initialize reaction counts", error)
    }

    /**
     * Batch operation to get reactions for multiple content items.
     *
     * This method optimizes performance by chunking requests and executing them.
     * Failed individual requests won't cause the entire batch to fail.
     *
     * @param contentType Type of content
     * @param contentIds List of content IDs to fetch reactions for
     * @return Result containing a map of content IDs to their reaction counts
     */
    suspend fun getBatchReactionCounts(
        contentType: ContentType,
        contentIds: List<String>
    ): Result<Map<String, ReactionCounts>> = runCatching {
        require(contentIds.isNotEmpty()) { "Content IDs list cannot be empty" }

        Log.d(TAG, "Fetching batch reaction counts for ${contentIds.size} items")

        val results = contentIds
            .distinct()
            .chunked(BATCH_CHUNK_SIZE)
            .flatMap { chunk ->
                chunk.map { contentId ->
                    val counts = getReactionCounts(contentType, contentId)
                        .getOrNull()
                        ?: ReactionCounts.empty()
                    contentId to counts
                }
            }
            .toMap()

        Log.d(TAG, "Successfully fetched ${results.size} reaction counts")
        results
    }.onFailure { error ->
        Log.e(TAG, "Failed to fetch batch reaction counts", error)
    }

    /**
     * Batch operation to get user reactions for multiple content items.
     *
     * @param contentIds List of content IDs to check
     * @param userId User ID to check reactions for
     * @return Result containing a map of content IDs to reaction types
     */
    suspend fun getBatchUserReactions(
        contentIds: List<String>,
        userId: String
    ): Result<Map<String, ReactionType?>> = runCatching {
        require(contentIds.isNotEmpty()) { "Content IDs list cannot be empty" }
        require(userId.isNotBlank()) { "User ID cannot be blank" }

        Log.d(TAG, "Fetching batch user reactions for ${contentIds.size} items")

        contentIds
            .distinct()
            .chunked(BATCH_CHUNK_SIZE)
            .flatMap { chunk ->
                chunk.map { contentId ->
                    val reaction = getUserReaction(contentId, userId).getOrNull()
                    contentId to reaction
                }
            }
            .toMap()
    }.onFailure { error ->
        Log.e(TAG, "Failed to fetch batch user reactions", error)
    }

    /**
     * Removes all reactions from a piece of content.
     * Use with caution - this is typically called when content is deleted.
     *
     * @param contentType Type of content
     * @param contentId Content ID to remove reactions from
     * @return Result indicating success or failure
     */
    suspend fun removeAllReactions(
        contentType: ContentType,
        contentId: String
    ): Result<Unit> = runCatching {
        require(contentId.isNotBlank()) { "Content ID cannot be blank" }

        Log.d(TAG, "Removing all reactions for $contentType=$contentId")

        // Delete the counts node
        val countsRef = getCountsRef(contentType.value, contentId)
        countsRef.removeValue().await()

        // Delete all user reaction nodes
        val userReactionsRef = db.reference
            .child(PATH_ACTIVITIES)
            .child(PATH_REACTIONS_ACTIVITY)
            .child(contentId)

        userReactionsRef.removeValue().await()

        Log.d(TAG, "Successfully removed all reactions for $contentType=$contentId")
        Unit
    }.onFailure { error ->
        Log.e(TAG, "Failed to remove all reactions", error)
    }
}