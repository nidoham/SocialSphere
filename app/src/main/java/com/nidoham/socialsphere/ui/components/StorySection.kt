package com.nidoham.socialsphere.ui.components

import android.util.Log
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.socialsphere.database.cloud.model.Story
import com.nidoham.socialsphere.database.cloud.model.User
import com.nidoham.socialsphere.database.cloud.repository.UserRepository
import com.nidoham.socialsphere.ui.viewmodel.StoryViewModel

private const val TAG = "StorySection"

/* --------------------------------------------------
   STORY SECTION
-------------------------------------------------- */

@Composable
fun StorySection(
    viewModel: StoryViewModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val stories by viewModel.stories.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val isLoadingStories by viewModel.isLoadingStories.collectAsState()

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            snackbarHostState.showSnackbar("Story published successfully!")
            viewModel.clearSuccess()
            showAddDialog = false
        }
    }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUploadError()
        }
    }

    // Log stories count
    LaunchedEffect(stories.size) {
        Log.d(TAG, "Displaying ${stories.size} stories")
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (isLoadingStories) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Stories horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add Story Item (first item)
            StoryItem(
                isAddStory = true,
                onClick = { showAddDialog = true }
            )

            // Regular Story Items - Clean version without reactions
            stories.forEach { story ->
                Log.d(TAG, "Rendering story: ${story.id}, userId: ${story.userId}")

                CleanStoryItem(
                    story = story,
                    onClick = {
                        viewModel.incrementViews(story.id)
                        Log.d(TAG, "Story clicked: ${story.id}")
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddStoryDialog(
            isUploading = isUploading,
            onDismiss = { if (!isUploading) showAddDialog = false },
            onUpload = { caption, imageFile ->
                FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                    viewModel.uploadStory(caption, imageFile, userId)
                }
            }
        )
    }
}

/* --------------------------------------------------
   CLEAN STORY ITEM (No reactions, just image + avatar + username)
-------------------------------------------------- */

@Composable
fun CleanStoryItem(
    story: Story,
    onClick: () -> Unit = {}
) {
    var storyUser by remember { mutableStateOf<User?>(null) }
    var isLoadingUser by remember { mutableStateOf(false) }

    LaunchedEffect(story.userId) {
        isLoadingUser = true
        try {
            Log.d(TAG, "Fetching user for story: ${story.userId}")
            storyUser = UserRepository.getInstance().getUserById(story.userId)
            Log.d(TAG, "User fetched: ${storyUser?.displayName}, avatar: ${storyUser?.avatarUrl}")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user: ${e.message}", e)
        } finally {
            isLoadingUser = false
        }
    }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(Color(0xFF1C1C1E))
    ) {
        // Story image background
        story.imageUrl?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(
                    model = url,
                    onError = {
                        Log.e(TAG, "Failed to load story image: $url")
                    }
                ),
                contentDescription = "Story image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // Profile picture with border - FIXED
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(42.dp)
                .border(3.dp, Color(0xFF1877F2), CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoadingUser -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1877F2)
                    )
                }
                !storyUser?.avatarUrl.isNullOrBlank() -> {
                    Log.d(TAG, "Displaying avatar: ${storyUser?.avatarUrl}")
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = storyUser?.avatarUrl,
                            onError = {
                                Log.e(TAG, "Failed to load avatar: ${storyUser?.avatarUrl}")
                            }
                        ),
                        contentDescription = "User avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Log.d(TAG, "Using default icon for user")
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default avatar",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Username at bottom
        Text(
            text = storyUser?.displayName
                ?: storyUser?.username
                ?: "Loading...",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp, 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            lineHeight = 14.sp
        )
    }
}