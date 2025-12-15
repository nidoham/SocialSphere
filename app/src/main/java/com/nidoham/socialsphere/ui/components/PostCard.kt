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

@Composable
fun PostCard(
    userName: String,
    timeAgo: String,
    content: String,
    likeCount: String,
    commentCount: String,
    shareCount: String,
    profilePicUrl: String? = null,
    postImageUrl: String? = null,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Post Header
            PostHeader(
                userName = userName,
                timeAgo = timeAgo,
                profilePicUrl = profilePicUrl,
                onMoreClick = onMoreClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Post Content with See More
            PostContent(content = content)

            Spacer(modifier = Modifier.height(12.dp))

            // Post Image with dynamic height
            if (postImageUrl != null) {
                PostImage(imageUrl = postImageUrl)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostActionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "Like",
                    count = likeCount,
                    onClick = onLikeClick
                )
                PostActionButton(
                    icon = Icons.Default.Comment,
                    label = "Comment",
                    count = commentCount,
                    onClick = onCommentClick
                )
                PostActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    count = shareCount,
                    onClick = onShareClick
                )
            }
        }
    }
}

@Composable
private fun PostHeader(
    userName: String,
    timeAgo: String,
    profilePicUrl: String?,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile picture with blue border
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
                if (profilePicUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePicUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "User",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    val maxLines = if (isExpanded) Int.MAX_VALUE else 2

    Column {
        Text(
            text = content,
            fontSize = 14.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )

        // Show "See More" if content is long
        if (content.length > 100 && !isExpanded) {
            Text(
                text = "See More",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { isExpanded = true }
            )
        }

        if (isExpanded) {
            Text(
                text = "See Less",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { isExpanded = false }
            )
        }
    }
}

@Composable
private fun PostImage(imageUrl: String) {
    var imageHeight by remember { mutableStateOf(300.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = imageUrl,
                onSuccess = { state ->
                    val intrinsicSize = state.painter.intrinsicSize
                    val aspectRatio = intrinsicSize.width / intrinsicSize.height

                    // Calculate height based on width (screen width - padding)
                    // Limit between 200dp and 500dp for reasonable display
                    val calculatedHeight = (intrinsicSize.height / intrinsicSize.width) * 400
                    imageHeight = calculatedHeight.dp.coerceIn(200.dp, 500.dp)
                }
            ),
            contentDescription = "Post Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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