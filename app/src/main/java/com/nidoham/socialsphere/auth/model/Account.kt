package com.nidoham.socialsphere.auth.model

import android.os.Parcelable
import com.google.firebase.auth.FirebaseUser
import com.nidoham.socialsphere.auth.model.settings.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class Account(
    val account: AccountSettings = AccountSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val timelineAndTagging: TimelineAndTaggingSettings = TimelineAndTaggingSettings(),
    val blocking: BlockingSettings = BlockingSettings(),
    val followersAndPublicContent: FollowersAndPublicContentSettings = FollowersAndPublicContentSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val languageAndRegion: LanguageAndRegionSettings = LanguageAndRegionSettings(),
    val mediaAndData: MediaAndDataSettings = MediaAndDataSettings(),
    val ads: AdsSettings = AdsSettings(),
    val yourInformation: YourInformationSettings = YourInformationSettings()
) : Parcelable {

    // No-argument constructor for Firebase
    constructor() : this(
        account = AccountSettings(),
        security = SecuritySettings(),
        privacy = PrivacySettings(),
        timelineAndTagging = TimelineAndTaggingSettings(),
        blocking = BlockingSettings(),
        followersAndPublicContent = FollowersAndPublicContentSettings(),
        notifications = NotificationSettings(),
        languageAndRegion = LanguageAndRegionSettings(),
        mediaAndData = MediaAndDataSettings(),
        ads = AdsSettings(),
        yourInformation = YourInformationSettings()
    )

    companion object {
        fun from(user: FirebaseUser): Account {
            return Account(
                account = AccountSettings(
                    username = user.displayName ?: "",
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    phoneNumber = user.phoneNumber,
                    createdAt = user.metadata?.creationTimestamp ?: 0
                ),
                security = SecuritySettings(
                    passwordLastChanged = 0,
                    twoFactorEnabled = false,
                    loginAlertsEnabled = false,
                    trustedDevices = emptyList(),
                    activeSessions = emptyList()
                ),
                privacy = PrivacySettings(
                    whoCanSeeMyPosts = "PUBLIC",
                    whoCanSeeMyFriendsList = "FRIENDS",
                    whoCanSendFriendRequests = "FRIENDS",
                    whoCanMessageMe = "FRIENDS"
                ),
                timelineAndTagging = TimelineAndTaggingSettings(
                    reviewTagsPeopleAdd = false,
                    reviewPostsYouAreTaggedIn = false,
                    whoCanPostOnYourTimeline = "FRIENDS",
                    whoCanSeeWhatOthersPost = "PUBLIC"
                ),
                blocking = BlockingSettings(
                    blockedUsers = emptyList(),
                    blockedPages = emptyList(),
                    blockedApps = emptyList(),
                ),
                followersAndPublicContent = FollowersAndPublicContentSettings(
                    whoCanFollowMe = "FRIENDS",
                    whoCanCommentOnPublicPosts = "FRIENDS",
                    publicPostNotificationsEnabled = false
                ),
                notifications = NotificationSettings(
                    pushNotifications = false,
                    emailNotifications = false,
                    smsNotifications = false,
                    notificationTypes = emptyList()
                ),
                languageAndRegion = LanguageAndRegionSettings(
                    language = "en",
                    region = "US",
                    timeZone = "America/New_York"
                ),
                mediaAndData = MediaAndDataSettings(
                    autoPlayVideos = false,
                    highQualityUploads = false,
                    dataSaverEnabled = false
                ),
                ads = AdsSettings(
                    personalizedAdsEnabled = false,
                    adInterests = emptyList()
                ),
                yourInformation = YourInformationSettings(
                    activityLogEnabled = false,
                    allowDataDownload = false,
                    allowDataTransfer = false
                )
            )
        }
    }
}