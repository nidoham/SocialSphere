package com.nidoham.socialsphere;

import java.util.Locale

enum class PostStatus(val value: String) {
    PUBLIC("public"),
    FRIENDS("friends"),
    PRIVATE("private"),
    UNLISTED("unlisted");

    companion object {
        // Create the map once. Keys are normalized to lowercase for case-insensitive lookup.
        private val map = entries.associateBy { it.value }

        @JvmStatic
        fun fromString(status: String?): PostStatus {
            // 1. Handle null/blank safely
            if (status.isNullOrBlank()) return PUBLIC
            
            // 2. Normalize input to match map keys (safe lowercase)
            val normalizedKey = status.lowercase(Locale.ROOT)
            
            // 3. Return match or fallback to default
            return map[normalizedKey] ?: PUBLIC
        }
    }
}