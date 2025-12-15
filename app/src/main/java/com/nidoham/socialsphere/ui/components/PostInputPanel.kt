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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PostInputPanel(
    onPostClick: () -> Unit = {},
    onPhotoClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onFeelingClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Get current user from Firebase
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName ?: "User"
    val userProfilePic = currentUser?.photoUrl?.toString()

    var postText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top section with avatar and text input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar with blue border
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
                    if (userProfilePic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(userProfilePic),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Actual Text Input Field
                TextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = {
                        Text(
                            text = "What's on your mind, $userName?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                // Send Icon Button
                IconButton(
                    onClick = {
                        if (postText.isNotBlank()) {
                            onPostClick()
                            postText = ""
                        }
                    },
                    enabled = postText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Post",
                        tint = if (postText.isNotBlank())
                            Color(0xFF1877F2)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                // Three Dots Menu
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostActionButton(
                    icon = Icons.Default.Image,
                    label = "Photo",
                    tint = Color(0xFF45BD62),
                    onClick = onPhotoClick
                )

                PostActionButton(
                    icon = Icons.Default.VideoLibrary,
                    label = "Video",
                    tint = Color(0xFFE42645),
                    onClick = onVideoClick
                )

                PostActionButton(
                    icon = Icons.Default.EmojiEmotions,
                    label = "Feeling",
                    tint = Color(0xFFF7B928),
                    onClick = onFeelingClick
                )
            }
        }
    }
}

@Composable
private fun PostActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}