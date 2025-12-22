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
import com.nidoham.socialsphere.extractor.ReactionCounts
import com.nidoham.socialsphere.extractor.ReactionType

/**
 * Enhanced Facebook/Instagram-style social media post component
 *
 * Features:
 * - Instagram-style gradient profile borders
 * - Story ring indicator support
 * - Multi-image carousel with indicators
 * - Video thumbnail support with play icon
 * - Engagement stats with profile avatars
 * - Like/Love reactions support
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
    val reactionCounts: ReactionCounts = ReactionCounts.empty(),
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val topComments: List<CommentData> = emptyList(),
    val likedByUsers: List<UserAvatar> = emptyList()
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
            Color(0xFFFCAF45),
            Color(0xFFF77737),
            Color(0xFFE1306C),
            Color(0xFFC13584),
            Color(0xFF833AB4)
        )
    )

    val loveGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF0844),
            Color(0xFFFF6B9D)
        )
    )

    val verifiedBlue = Color(0xFF1DA1F2)
    val goldVerified = Color(0xFFFFD700)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialMediaPostItem(
    post: PostData,
    userReaction: ReactionType? = null,
    isBookmarked: Boolean = false,
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
    onReactionClick: (String, ReactionType) -> Unit = { _, _ -> },
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
    var showReactionPicker by remember { mutableStateOf(false) }

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
            containerColor = MaterialTheme.colorScheme.background
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
                    userReaction = userReaction,
                    doubleTapLike = doubleTapLike,
                    likeScale = likeScale,
                    onDoubleTap = {
                        if (userReaction == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReactionClick(post.postId, ReactionType.Like)
                        }
                        doubleTapLike = true
                    },
                    onTap = { onPostClick(post.postId) }
                )
            }

            // Engagement Stats
            EngagementStats(
                reactionCounts = post.reactionCounts,
                commentsCount = post.commentsCount,
                sharesCount = post.sharesCount,
                likedByUsers = post.likedByUsers
            )

            // Action Buttons
            PostActions(
                userReaction = userReaction,
                isBookmarked = isBookmarked,
                showReactionPicker = showReactionPicker,
                onToggleReactionPicker = { showReactionPicker = !showReactionPicker },
                onReactionClick = { reaction ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onReactionClick(post.postId, reaction)
                    showReactionPicker = false
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (hasActiveStory) {
                            InstagramColors.storyGradient
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE1306C).copy(alpha = 0.7f),
                                    Color(0xFFC13584).copy(alpha = 0.7f),
                                    Color(0xFF833AB4).copy(alpha = 0.7f)
                                )
                            )
                        },
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InstagramColors.verifiedBlue, CircleShape)
        )
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
    userReaction: ReactionType?,
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
                    .pointerInput(userReaction) {
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
    reactionCounts: ReactionCounts,
    commentsCount: Int,
    sharesCount: Int,
    likedByUsers: List<UserAvatar>
) {
    val totalReactions = reactionCounts.likes + reactionCounts.loves

    if (totalReactions > 0 || commentsCount > 0 || sharesCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (totalReactions > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reaction icons
                    Row(horizontalArrangement = Arrangement.spacedBy((-1).dp)) {
                        if (reactionCounts.likes > 0) {
                            ReactionIcon(
                                icon = Icons.Filled.ThumbUp,
                                color = Color.Yellow,
                                size = 20.dp
                            )
                        }
                        if (reactionCounts.loves > 0) {
                            ReactionIcon(
                                icon = Icons.Filled.Favorite,
                                color = Color.Red,
                                size = 20.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = buildString {
                            if (likedByUsers.isNotEmpty()) {
                                append(likedByUsers[0].username)
                                if (totalReactions > 1) {
                                    append(" and ${formatCount((totalReactions - 1).toInt())} ${if (totalReactions == 2L) "other" else "others"}")
                                }
                            } else {
                                append("${formatCount(totalReactions.toInt())} ${if (totalReactions == 1L) "reaction" else "reactions"}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
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
private fun ReactionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        )
    }
}

@Composable
private fun PostActions(
    userReaction: ReactionType?,
    isBookmarked: Boolean,
    showReactionPicker: Boolean,
    onToggleReactionPicker: () -> Unit,
    onReactionClick: (ReactionType) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Column {
        // Reaction Picker Dropdown (positioned above the action buttons)
        androidx.compose.animation.AnimatedVisibility(
            visible = showReactionPicker,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 4.dp)
                    .wrapContentSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { onReactionClick(ReactionType.Like) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "Like",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { onReactionClick(ReactionType.Love) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Love",
                            tint = Color(0xFFED4956),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reaction Button (Like/Love)
            TextButton(
                onClick = {
                    if (userReaction == null) {
                        onReactionClick(ReactionType.Like)
                    } else {
                        onToggleReactionPicker()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = when (userReaction) {
                            ReactionType.Like -> "Liked post"
                            ReactionType.Love -> "Loved post"
                            null -> "React to post"
                        }
                    }
            ) {
                Icon(
                    imageVector = when (userReaction) {
                        ReactionType.Like -> Icons.Filled.ThumbUp
                        ReactionType.Love -> Icons.Filled.Favorite
                        null -> Icons.Outlined.ThumbUpOffAlt
                    },
                    contentDescription = null,
                    tint = when (userReaction) {
                        ReactionType.Like -> MaterialTheme.colorScheme.primary
                        ReactionType.Love -> Color(0xFFED4956)
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (userReaction) {
                        ReactionType.Like -> "Like"
                        ReactionType.Love -> "Love"
                        null -> "Like"
                    },
                    color = when (userReaction) {
                        ReactionType.Like -> MaterialTheme.colorScheme.primary
                        ReactionType.Love -> Color(0xFFED4956)
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (userReaction != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Comment Button
            TextButton(
                onClick = onCommentClick,
                modifier = Modifier.semantics {
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