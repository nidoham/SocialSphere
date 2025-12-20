package com.nidoham.socialsphere.ui.item

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Enhanced Facebook/Instagram-style social media post component
 *
 * Features:
 * - Instagram-style gradient profile borders
 * - Story ring indicator support
 * - Multi-image carousel with indicators
 * - Video thumbnail support with play icon
 * - Engagement stats with profile avatars
 * - Comments preview section
 * - Privacy indicator
 * - Enhanced verified badges
 * - Hashtag and mention support
 * - Material 3 Card design with elevation
 * - Smooth animations and haptic feedback
 */

data class PostData(
    val postId: String,
    val authorUsername: String,
    val authorAvatar: String?,
    val isAuthorVerified: Boolean = false,
    val hasActiveStory: Boolean = false,
    val timestamp: Long,
    val privacy: PostPrivacy = PostPrivacy.PUBLIC,
    val caption: String? = null,
    val mediaUrls: List<MediaItem> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val topComments: List<CommentData> = emptyList(),
    val likedByUsers: List<UserAvatar> = emptyList() // New: Show who liked
)

data class MediaItem(
    val url: String,
    val type: MediaType = MediaType.IMAGE
)

data class UserAvatar(
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean = false
)

enum class MediaType {
    IMAGE, VIDEO
}

enum class PostPrivacy(val icon: @Composable () -> Unit, val label: String) {
    PUBLIC({ Icon(Icons.Default.Public, null, Modifier.size(14.dp)) }, "Public"),
    FRIENDS({ Icon(Icons.Default.Group, null, Modifier.size(14.dp)) }, "Friends"),
    ONLY_ME({ Icon(Icons.Default.Lock, null, Modifier.size(14.dp)) }, "Only me")
}

data class CommentData(
    val username: String,
    val text: String,
    val isVerified: Boolean = false
)

// Instagram-style gradient colors
object InstagramColors {
    val storyGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFCAF45), // Orange
            Color(0xFFF77737), // Red-Orange
            Color(0xFFE1306C), // Pink
            Color(0xFFC13584), // Purple
            Color(0xFF833AB4)  // Deep Purple
        )
    )

    val liveGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD300C5), // Magenta
            Color(0xFFFF0069), // Hot Pink
        )
    )

    val verifiedBlue = Color(0xFF1DA1F2)
    val goldVerified = Color(0xFFFFD700)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialMediaPostItem(
    post: PostData,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
    onLikeClick: (String, Boolean) -> Unit = { _, _ -> },
    onCommentClick: (String) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    onBookmarkClick: (String, Boolean) -> Unit = { _, _ -> },
    onProfileClick: (String) -> Unit = {},
    onMoreClick: (String) -> Unit = {},
    onViewAllComments: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showFullCaption by remember { mutableStateOf(false) }
    var doubleTapLike by remember { mutableStateOf(false) }

    val likeScale by animateFloatAsState(
        targetValue = if (doubleTapLike) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 250f),
        finishedListener = { doubleTapLike = false },
        label = "like_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = false) {
                    contentDescription = "Post by ${post.authorUsername}"
                }
        ) {
            // Header
            PostHeader(
                authorUsername = post.authorUsername,
                authorAvatar = post.authorAvatar,
                isVerified = post.isAuthorVerified,
                hasActiveStory = post.hasActiveStory,
                timestamp = formatTimestamp(post.timestamp),
                privacy = post.privacy,
                onProfileClick = { onProfileClick(post.authorUsername) },
                onMoreClick = { onMoreClick(post.postId) }
            )

            // Caption
            if (!post.caption.isNullOrEmpty()) {
                PostCaption(
                    authorUsername = post.authorUsername,
                    caption = post.caption,
                    showFullCaption = showFullCaption,
                    onToggleCaption = { showFullCaption = !showFullCaption }
                )
            }

            // Media (Images/Videos)
            if (post.mediaUrls.isNotEmpty()) {
                PostMediaCarousel(
                    mediaItems = post.mediaUrls,
                    isLiked = isLiked,
                    doubleTapLike = doubleTapLike,
                    likeScale = likeScale,
                    onDoubleTap = {
                        if (!isLiked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick(post.postId, true)
                        }
                        doubleTapLike = true
                    },
                    onTap = { onPostClick(post.postId) }
                )
            }

            // Engagement Stats
            EngagementStats(
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                sharesCount = post.sharesCount,
                likedByUsers = post.likedByUsers
            )

            // Action Buttons
            PostActions(
                isLiked = isLiked,
                isBookmarked = isBookmarked,
                onLikeClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLikeClick(post.postId, !isLiked)
                },
                onCommentClick = { onCommentClick(post.postId) },
                onShareClick = { onShareClick(post.postId) },
                onBookmarkClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBookmarkClick(post.postId, !isBookmarked)
                }
            )

            // Comments Preview
            if (post.topComments.isNotEmpty()) {
                CommentsPreview(
                    comments = post.topComments,
                    totalCommentsCount = post.commentsCount,
                    onViewAllComments = { onViewAllComments(post.postId) }
                )
            }
        }
    }
}

@Composable
private fun PostHeader(
    authorUsername: String,
    authorAvatar: String?,
    isVerified: Boolean,
    hasActiveStory: Boolean,
    timestamp: String,
    privacy: PostPrivacy,
    onProfileClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture with Instagram-style gradient ring
        Box(
            modifier = Modifier
                .size(50.dp)
                .clickable(onClick = onProfileClick)
        ) {
            // Always show gradient ring (more prominent if has active story)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (hasActiveStory) {
                            InstagramColors.storyGradient
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE1306C).copy(alpha = 0.7f), // Pink
                                    Color(0xFFC13584).copy(alpha = 0.7f), // Purple
                                    Color(0xFF833AB4).copy(alpha = 0.7f)  // Deep Purple
                                )
                            )
                        },
                        shape = CircleShape
                    )
            )

            // White padding ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(authorAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture of $authorUsername",
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.Center),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Username + Timestamp + Privacy
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onProfileClick)
                .padding(vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = authorUsername,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(size = 18)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                privacy.icon()
            }
        }

        // More Options Button
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VerifiedBadge(
    size: Int = 16,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size.dp)
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InstagramColors.verifiedBlue, CircleShape)
        )
        // Checkmark
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Verified account",
            tint = Color.White,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        )
    }
}

@Composable
private fun PostCaption(
    authorUsername: String,
    caption: String,
    showFullCaption: Boolean,
    onToggleCaption: () -> Unit
) {
    val captionText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            //append(authorUsername)
        }
        append(" ")

        // Parse caption for hashtags and mentions
        val words = caption.split(" ")
        words.forEachIndexed { index, word ->
            when {
                word.startsWith("#") -> {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(word)
                    }
                }
                word.startsWith("@") -> {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(word)
                    }
                }
                else -> {
                    append(word)
                }
            }
            if (index < words.size - 1) append(" ")
        }
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            text = captionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (showFullCaption) Int.MAX_VALUE else 3,
            overflow = if (showFullCaption) TextOverflow.Visible else TextOverflow.Ellipsis
        )

        if (caption.length > 150) {
            Text(
                text = if (showFullCaption) "See less" else "See more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onToggleCaption)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostMediaCarousel(
    mediaItems: List<MediaItem>,
    isLiked: Boolean,
    doubleTapLike: Boolean,
    likeScale: Float,
    onDoubleTap: () -> Unit,
    onTap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 500.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mediaItem = mediaItems[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLiked) {
                        detectTapGestures(
                            onDoubleTap = { onDoubleTap() },
                            onTap = { onTap() }
                        )
                    }
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaItem.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post media",
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to load media",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Video play icon overlay
                if (mediaItem.type == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // Double tap like animation
                AnimatedVisibility(
                    visible = doubleTapLike,
                    enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                    exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .scale(likeScale)
                    )
                }
            }
        }

        // Page indicators (dots)
        if (mediaItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(mediaItems.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EngagementStats(
    likesCount: Int,
    commentsCount: Int,
    sharesCount: Int,
    likedByUsers: List<UserAvatar>
) {
    if (likesCount > 0 || commentsCount > 0 || sharesCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (likesCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Show actual profile avatars of people who liked
                    val usersToShow = likedByUsers.take(3)

                    if (usersToShow.isNotEmpty()) {
                        Box {
                            usersToShow.forEachIndexed { index, user ->
                                ProfileAvatarWithGradient(
                                    avatarUrl = user.avatarUrl,
                                    username = user.username,
                                    isVerified = user.isVerified,
                                    size = 26.dp,
                                    modifier = Modifier.padding(start = (index * 14).dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width((usersToShow.size * 16 + 8).dp))
                    } else {
                        // Fallback to heart icons if no user data
                        Box {
                            repeat(minOf(3, likesCount)) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = (index * 12).dp)
                                        .size(26.dp)
                                        .background(
                                            brush = InstagramColors.storyGradient,
                                            shape = CircleShape
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(2.dp)
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFED4956),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(if (likesCount > 1) 50.dp else 8.dp))
                    }

                    Text(
                        text = if (usersToShow.isNotEmpty() && usersToShow.size == 1) {
                            "Liked by ${usersToShow[0].username}${if (likesCount > 1) " and ${likesCount - 1} ${if (likesCount == 2) "other" else "others"}" else ""}"
                        } else if (usersToShow.size > 1) {
                            "Liked by ${usersToShow[0].username}, ${usersToShow[1].username}${if (likesCount > 2) " and ${likesCount - 2} ${if (likesCount == 3) "other" else "others"}" else ""}"
                        } else {
                            "Liked by ${formatCount(likesCount)} ${if (likesCount == 1) "person" else "people"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (commentsCount > 0) {
                    Text(
                        text = "${formatCount(commentsCount)} comments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (sharesCount > 0) {
                    Text(
                        text = "${formatCount(sharesCount)} shares",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ProfileAvatarWithGradient(
    avatarUrl: String?,
    username: String,
    isVerified: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        // Gradient ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = InstagramColors.storyGradient,
                    shape = CircleShape
                )
        )

        // White padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar of $username",
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size((size.value * 0.6f).dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Verified badge overlay
        if (isVerified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                VerifiedBadge(size = (size.value * 0.45f).toInt())
            }
        }
    }
}

@Composable
private fun PostActions(
    isLiked: Boolean,
    isBookmarked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like Button
        TextButton(
            onClick = onLikeClick,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = if (isLiked) "Unlike post" else "Like post"
                }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUpOffAlt,
                contentDescription = null,
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Like",
                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLiked) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        // Comment Button
        TextButton(
            onClick = onCommentClick,
            modifier = Modifier
                .semantics {
                    contentDescription = "Comment on post"
                }
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Comment",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Share Button
        TextButton(
            onClick = onShareClick,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = "Share post"
                }
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Share",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Bookmark Button
        IconButton(
            onClick = onBookmarkClick,
            modifier = Modifier.semantics {
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark post"
            }
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun CommentsPreview(
    comments: List<CommentData>,
    totalCommentsCount: Int,
    onViewAllComments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        comments.take(2).forEach { comment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val commentText = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(comment.username)
                    }
                    append(" ")
                    append(comment.text)
                }

                Text(
                    text = commentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (comment.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(size = 14)
                }
            }
        }

        if (totalCommentsCount > 2) {
            Text(
                text = "View all $totalCommentsCount comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onViewAllComments)
                    .padding(vertical = 6.dp)
            )
        }
    }
}

// Utility Functions

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 0 -> "Just now"
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        diff < 2592000_000 -> "${diff / 604800_000}w"
        else -> "${diff / 2592000_000}mo"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> "$count"
        count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1_000_000.0)
    }
}