package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivacySettings(
    val whoCanSeeMyPosts: String = "PUBLIC",          // PUBLIC / FRIENDS / ONLY_ME
    val whoCanSeeMyFriendsList: String = "FRIENDS",
    val whoCanSendFriendRequests: String = "FRIENDS",
    val whoCanMessageMe: String = "FRIENDS"
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this("PUBLIC", "FRIENDS", "FRIENDS", "FRIENDS")
}