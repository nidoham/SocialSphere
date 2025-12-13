package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdsSettings(
    val personalizedAdsEnabled: Boolean = false,
    val adInterests: List<String> = emptyList()
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(false, emptyList())
}