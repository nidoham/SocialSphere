package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AccountSettings(
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val createdAt: Long = 0,
    val profilePictureUrl: String? = ""
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", null, 0)
}