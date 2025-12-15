package com.nidoham.socialsphere.posts.model

enum class PostContentCategory(
    val hasText: Boolean,
    val hasMedia: Boolean
) {
    TEXT_ONLY(hasText = true, hasMedia = false),
    IMAGE_ONLY(hasText = false, hasMedia = true),
    VIDEO_ONLY(hasText = false, hasMedia = true),
    TEXT_WITH_IMAGE(hasText = true, hasMedia = true),
    TEXT_WITH_VIDEO(hasText = true, hasMedia = true),
    MIXED_MEDIA(hasText = true, hasMedia = true), // Assuming mixed implies text context, or set false if not
    UNKNOWN(hasText = false, hasMedia = false);

    /**
     * Optional: Helper to check specific media complexity
     */
    val isMixed: Boolean
        get() = this == MIXED_MEDIA

    companion object {

        /**
         * Determines the category based on content flags.
         * Uses explicit logic flow which is safer and easier to debug
         * than bitwise/array lookups.
         */
        @JvmStatic
        fun determine(
            hasText: Boolean,
            hasImage: Boolean,
            hasVideo: Boolean
        ): PostContentCategory {
            // 1. Check for Mixed Media (Both Image + Video)
            if (hasImage && hasVideo) {
                return MIXED_MEDIA
            }

            // 2. Check for Single Media Types
            if (hasImage) {
                return if (hasText) TEXT_WITH_IMAGE else IMAGE_ONLY
            }

            if (hasVideo) {
                return if (hasText) TEXT_WITH_VIDEO else VIDEO_ONLY
            }

            // 3. Check for Text Only or Unknown
            return if (hasText) TEXT_ONLY else UNKNOWN
        }
    }
}