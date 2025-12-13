package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FollowersAndPublicContentSettings(
    val whoCanFollowMe: String = "FRIENDS",          // PUBLIC / FRIENDS
    val whoCanCommentOnPublicPosts: String = "FRIENDS",
    val publicPostNotificationsEnabled: Boolean = false
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this("FRIENDS", "FRIENDS", false)
}