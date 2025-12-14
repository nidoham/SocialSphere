package com.nidoham.socialsphere.stories.model

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val headline: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reactions: List<String> = emptyList(),
    val banned: List<String> = emptyList()
)