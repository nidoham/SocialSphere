package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LanguageAndRegionSettings(
    val language: String = "en",                // bn / en
    val region: String = "US",
    val timeZone: String = "America/New_York"
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this("en", "US", "America/New_York")
}