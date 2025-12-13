package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlockingSettings(
    val blockedUsers: List<String> = emptyList(),
    val blockedPages: List<String> = emptyList(),
    val blockedApps: List<String> = emptyList()
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(emptyList(), emptyList(), emptyList())
}