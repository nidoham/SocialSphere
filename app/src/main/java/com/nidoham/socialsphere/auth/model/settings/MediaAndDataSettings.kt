package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaAndDataSettings(
    val autoPlayVideos: Boolean = false,
    val highQualityUploads: Boolean = false,
    val dataSaverEnabled: Boolean = false
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(false, false, false)
}