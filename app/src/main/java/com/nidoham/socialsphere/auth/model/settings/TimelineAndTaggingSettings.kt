package com.nidoham.socialsphere.auth.model.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimelineAndTaggingSettings(
    val reviewTagsPeopleAdd: Boolean = false,
    val reviewPostsYouAreTaggedIn: Boolean = false,
    val whoCanPostOnYourTimeline: String = "FRIENDS",
    val whoCanSeeWhatOthersPost: String = "PUBLIC"
) : Parcelable {
    // No-argument constructor for Firebase
    constructor() : this(false, false, "FRIENDS", "PUBLIC")
}