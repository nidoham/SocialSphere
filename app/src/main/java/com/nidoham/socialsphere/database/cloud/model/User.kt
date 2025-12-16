package com.nidoham.socialsphere.database.cloud.model

import com.google.firebase.auth.FirebaseUser

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String? = "",

    val email: String? = null,
    val phone: String? = null,

    // Profile media
    val avatarUrl: String? = null,
    val coverPhotoUrl: String? = null,

    // Bio & privacy
    val bio: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,

    val isPrivateAccount: Boolean = false,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,

    // Stats
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,

    // Account metadata
    val accountType: String = "personal", // personal, business, creator
    val accountStatus: String = "active", // active, suspended, deactivated
    val isPremium: Boolean = false,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val lastLoginAt: Long = 0L,
    val lastActiveAt: Long = 0L
) {
    companion object {
        fun fromFirebaseUser(user: FirebaseUser): User {
            return User(
                id = user.uid,
                username = "",
                displayName = user.displayName,
                email = user.email,
                phone = user.phoneNumber,
                avatarUrl = user.photoUrl?.toString(),
                isVerified = user.isEmailVerified
            )
        }
    }
}