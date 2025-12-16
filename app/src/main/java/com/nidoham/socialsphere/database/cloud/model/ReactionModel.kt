package com.nidoham.socialsphere.database.cloud.model

data class ReactionModel(
    val userId: String = "",
    val reaction: String = "",
    val timestamp: Long = 0L
)