package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SecuritySettings(
    val passwordLastChanged: Long = 0,
    val twoFactorEnabled: Boolean = false,
    val loginAlertsEnabled: Boolean = false,
    val trustedDevices: List<String> = emptyList(),
    val activeSessions: List<String> = emptyList()
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(0, false, false, emptyList(), emptyList())
}