package com.nidoham.socialsphere.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidoham.socialsphere.stories.model.Story
import kotlinx.coroutines.tasks.await

@Composable
fun StoryItem(
    isAddStory: Boolean = false,
    story: Story? = null,
    onClick: () -> Unit
) {
    // For Add Story - use current user
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserDisplayName = currentUser?.displayName ?: "User"
    val currentUserProfilePic = currentUser?.photoUrl?.toString()

    // For regular stories - fetch user data from Firestore
    var storyUserName by remember { mutableStateOf("Loading...") }
    var storyUserProfilePic by remember { mutableStateOf<String?>(null) }
    var isLoadingUser by remember { mutableStateOf(false) }

    // Fetch user data if it's a regular story
    LaunchedEffect(story?.userId) {
        if (!isAddStory && story != null) {
            isLoadingUser = true
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(story.userId)
                    .get()
                    .await()

                storyUserName = userDoc.getString("account.name")
                    ?: userDoc.getString("account.username")
                            ?: "Unknown User"
                storyUserProfilePic = userDoc.getString("account.profilePictureUrl")
            } catch (e: Exception) {
                storyUserName = "Unknown User"
            } finally {
                isLoadingUser = false
            }
        }
    }

    if (isAddStory) {
        // Add Story Design - Vertical split layout
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top section with user profile picture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUserProfilePic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(currentUserProfilePic),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Bottom section with "Create Story" text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Story",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Floating Add button at the divider between profile pic and gray bg
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (180.dp * 0.65f - 18.dp))
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Regular Story Design - Original layout
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .background(Color(0xFF1C1C1E))
        ) {
            // Story background image
            if (story?.imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(story.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay for better text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            // Top-left avatar with profile picture
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF1877F2), // Facebook blue border
                            shape = CircleShape
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingUser) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (storyUserProfilePic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(storyUserProfilePic),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom username
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = storyUserName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    lineHeight = 14.sp
                )
            }
        }
    }
}