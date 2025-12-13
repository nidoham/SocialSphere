package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YourInformationSettings(
    val activityLogEnabled: Boolean = false,
    val allowDataDownload: Boolean = false,
    val allowDataTransfer: Boolean = false
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(false, false, false)
}