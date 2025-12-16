package com.nidoham.socialsphere.database.cloud.model

data class Reaction(
    val likes: Long = 0,
    val dislikes: Long = 0
) {
    companion object {
        fun empty(): Reaction = Reaction()
    }

    fun total(): Long = likes + dislikes

    fun hasReactions(): Boolean = total() > 0

    fun likePercentage(): Double {
        if (total() == 0L) return 0.0
        return (likes.toDouble() / total()) * 100
    }
}