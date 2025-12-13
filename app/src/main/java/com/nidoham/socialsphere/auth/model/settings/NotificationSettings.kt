package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationSettings(
    val pushNotifications: Boolean = false,
    val emailNotifications: Boolean = false,
    val smsNotifications: Boolean = false,
    val notificationTypes: List<String> = emptyList()  // LIKE, COMMENT, FRIEND_REQUEST
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(false, false, false, emptyList())
}