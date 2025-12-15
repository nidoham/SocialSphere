package com.nidoham.socialsphere.posts.model

import com.nidoham.socialsphere.reaction.Reactions

data class Post(
    val postId: String = "",
    val userId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val reactions: Reactions,
    val commentsCount: Long,
    val sharesCount: Long,
    val banned: Boolean = false,
    val status: String = "PUBLIC",
    val hidden: Boolean = false,
    val deleted: Boolean = false
)