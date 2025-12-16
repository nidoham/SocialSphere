package com.nidoham.socialsphere.database.cloud.model

/**
 * Represents a social media post entity.
 *
 * @property id Unique identifier for the post
 * @property authorId ID of the user who created the post
 * @property content Text content of the post
 * @property contentType Type of content (TEXT, IMAGE, VIDEO, LINK, etc.)
 * @property mediaUrls List of media URLs attached to the post
 * @property reactions Aggregated reactions data for the post
 * @property visibility Post visibility setting (PUBLIC, FRIENDS, PRIVATE, CUSTOM)
 * @property location Optional geographic location where post was created
 * @property commentCount Total number of comments on the post
 * @property shareCount Total number of times the post has been shared
 * @property isEdited Whether the post has been edited after creation
 * @property parentPostId ID of parent post if this is a share/repost
 * @property parentPageId ID of parent page if posted on a page
 * @property parentGroupId ID of parent group if posted in a group
 * @property createdAt Timestamp when the post was created (milliseconds)
 * @property updatedAt Timestamp when the post was last updated (milliseconds)
 * @property deletedAt Timestamp when the post was deleted (milliseconds, 0 if not deleted)
 * @property status Current status of the post (ACTIVE, DELETED, ARCHIVED, HIDDEN)
 * @property engagementScore Calculated engagement score for ranking/recommendation
 * @property hashtags List of hashtags extracted from content
 * @property mentions List of user IDs mentioned in the post
 * @property embeddedLinks List of URLs embedded in the content
 * @property language ISO 639-1 language code of the content
 * @property isSponsored Whether this is a sponsored/promoted post
 */
data class Post(
    val id: String = "",
    val authorId: String = "",
    val content: String = "",
    val contentType: ContentType = ContentType.TEXT,
    val mediaUrls: List<String> = emptyList(),
    val reactions: Reaction = Reaction.empty(),
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val location: String? = null,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val isEdited: Boolean = false,
    val parentPostId: String? = null,
    val parentPageId: String? = null,
    val parentGroupId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long = 0,
    val status: PostStatus = PostStatus.ACTIVE,
    val engagementScore: Long = 0,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val embeddedLinks: List<String> = emptyList(),
    val language: String? = null,
    val isSponsored: Boolean = false
) {
    /**
     * Check if the post is currently active and visible
     */
    fun isActive(): Boolean = status == PostStatus.ACTIVE && deletedAt == 0L

    /**
     * Check if the post has been deleted
     */
    fun isDeleted(): Boolean = deletedAt > 0 || status == PostStatus.DELETED

    /**
     * Check if the post has any media attachments
     */
    fun hasMedia(): Boolean = mediaUrls.isNotEmpty()

    /**
     * Get total engagement count (reactions + comments + shares)
     */
    fun getTotalEngagement(): Long = reactions.total() + commentCount + shareCount

    /**
     * Check if the post has any engagement (reactions, comments, or shares)
     */
    fun hasEngagement(): Boolean = reactions.hasReactions() || commentCount > 0 || shareCount > 0

    /**
     * Get the total reaction count (likes + dislikes)
     */
    fun getReactionCount(): Long = reactions.total()
}

/**
 * Enum representing different types of post content
 */
enum class ContentType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    LINK,
    POLL,
    EVENT,
    ALBUM
}

/**
 * Enum representing post visibility settings
 */
enum class PostVisibility {
    PUBLIC,      // Visible to everyone
    FRIENDS,     // Visible to friends only
    PRIVATE,     // Visible only to the author
    CUSTOM       // Custom visibility settings
}

/**
 * Enum representing post status
 */
enum class PostStatus {
    ACTIVE,      // Post is active and visible
    DELETED,     // Post has been deleted
    ARCHIVED,    // Post has been archived
    HIDDEN,      // Post is hidden by moderator
    PENDING      // Post is pending review
}