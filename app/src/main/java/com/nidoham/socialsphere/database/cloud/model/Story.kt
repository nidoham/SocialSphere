package com.nidoham.socialsphere.database.cloud.model

data class Story(
    val id: String = "",
    val userId: String = "",
    val caption: String = "",

    val imageUrl: String? = null,
    val videoUrl: String? = null,

    val createdAt: Long = 0L,
    val expiresAt: Long = 0L, // Stories typically expire after 24 hours

    val isBanned: Boolean = false,

    // Engagement metrics
    val viewCount: Long = 0L,
    val replyCount: Long = 0L,
    val shareCount: Long = 0L,

    // Reactions using Reaction model
    val reactions: Reaction = Reaction(),

    // Privacy settings
    val visibility: String = "public", // public, friends, custom
    val allowedViewers: List<String> = emptyList(), // User IDs who can view (for custom visibility)

    // Story metadata
    val duration: Int = 5, // Duration in seconds for each story slide
    val backgroundColor: String? = null, // Background color for text-only stories
    val musicUrl: String? = null // Background music URL
) {
    companion object {
        const val VISIBILITY_PUBLIC = "public"
        const val VISIBILITY_FRIENDS = "friends"
        const val VISIBILITY_CUSTOM = "custom"

        const val DEFAULT_EXPIRY_HOURS = 24L

        fun getExpiryTimestamp(createdAt: Long = System.currentTimeMillis()): Long {
            return createdAt + (DEFAULT_EXPIRY_HOURS * 60 * 60 * 1000)
        }
    }

    // Helper methods
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    fun isActive(): Boolean {
        return !isExpired() && !isBanned
    }

    fun hasMedia(): Boolean {
        return !imageUrl.isNullOrBlank() || !videoUrl.isNullOrBlank()
    }

    fun getLikeCount(): Long {
        return reactions.likes
    }

    fun getDislikeCount(): Long {
        return reactions.dislikes
    }

    fun getTotalReactions(): Long {
        return reactions.likes + reactions.dislikes
    }
}