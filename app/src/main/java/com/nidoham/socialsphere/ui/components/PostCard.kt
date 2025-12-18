package com.nidoham.socialsphere.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.nidoham.social.model.User
import com.nidoham.social.repository.UserRepository
import com.nidoham.socialsphere.database.cloud.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PostCard(
    post: Post,
    timeAgo: String,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    userRepository: UserRepository = UserRepository() // Inject repository
) {
    // State to hold the fetched author details
    var authorName by remember { mutableStateOf("Loading...") }
    var authorAvatarUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Fetch user details when the composable enters composition or authorId changes
    LaunchedEffect(post.authorId) {
        isLoading = true
        hasError = false

        try {
            // Switch to IO dispatcher for network operations
            withContext(Dispatchers.IO) {
                val result = userRepository.getUserById(post.authorId)
                result.onSuccess { user ->
                    if (user != null) {
                        // Access nested profile properties correctly
                        authorName = user.profile.displayName.takeIf { it.isNotBlank() }
                            ?: user.profile.username
                        authorAvatarUrl = user.profile.avatarUrl.takeIf { it.isNotBlank() }
                    } else {
                        authorName = "Unknown User"
                        hasError = true
                    }
                }.onFailure {
                    authorName = "Unknown User"
                    hasError = true
                }
            }
        } catch (e: Exception) {
            authorName = "Unknown User"
            hasError = true
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Post Header with fetched user data
            PostHeader(
                userName = authorName,
                timeAgo = timeAgo,
                profilePicUrl = authorAvatarUrl,
                onMoreClick = onMoreClick,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Post Content with See More
            if (post.content.isNotBlank()) {
                PostContent(content = post.content)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Image - Check if mediaUrls list is not empty
            if (post.mediaUrls.isNotEmpty()) {
                post.mediaUrls.firstOrNull()?.let { imageUrl ->
                    if (imageUrl.isNotBlank()) {
                        PostImage(imageUrl = imageUrl)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Post Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostActionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "Like",
                    count = formatCount(post.reactions.likes),
                    onClick = onLikeClick
                )
                PostActionButton(
                    icon = Icons.Default.Comment,
                    label = "Comment",
                    count = formatCount(post.commentCount),
                    onClick = onCommentClick
                )
                PostActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    count = formatCount(post.shareCount),
                    onClick = onShareClick
                )
            }
        }
    }
}

/**
 * Helper to format large numbers (e.g., 1200 -> 1.2k)
 */
private fun formatCount(count: Long): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> "${String.format("%.1f", count / 1000.0)}k"
        else -> "${String.format("%.1f", count / 1000000.0)}M"
    }
}

@Composable
private fun PostHeader(
    userName: String,
    timeAgo: String,
    profilePicUrl: String?,
    onMoreClick: () -> Unit,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF1877F2), // Facebook blue
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    profilePicUrl != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(profilePicUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "User",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = timeAgo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun PostContent(content: String) {
    var isExpanded by remember { mutableStateOf(false) }
    // If expanded show all lines, otherwise limit to 3
    val maxLines = if (isExpanded) Int.MAX_VALUE else 3

    Column {
        Text(
            text = content,
            fontSize = 14.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Simple logic: if content is strictly longer than char limit, show toggle
        if (content.length > 150) {
            Text(
                text = if (isExpanded) "See Less" else "See More",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
private fun PostImage(imageUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 500.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = imageUrl,
                contentScale = ContentScale.Crop
            ),
            contentDescription = "Post Image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun PostActionButton(
    icon: ImageVector,
    label: String,
    count: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}