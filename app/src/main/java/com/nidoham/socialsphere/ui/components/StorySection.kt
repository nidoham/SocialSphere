package com.nidoham.socialsphere.ui.components

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
import com.nidoham.socialsphere.database.cloud.model.Reaction
import com.nidoham.socialsphere.database.cloud.model.Story
import com.nidoham.socialsphere.database.cloud.model.User
import com.nidoham.socialsphere.database.cloud.repository.UserRepository
import com.nidoham.socialsphere.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()

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

            // Regular Story Items
            stories.forEach { story ->
                var reaction by remember { mutableStateOf(Reaction()) }
                var isUserLiked by remember { mutableStateOf(false) }
                var isReactionLoading by remember { mutableStateOf(false) }

                LaunchedEffect(story.id) {
                    reaction = viewModel.getReactionCounts(story.id)
                    isUserLiked = viewModel.hasUserLiked(story.id)
                }

                StoryItemWithReactions(
                    story = story,
                    reaction = reaction,
                    isUserLiked = isUserLiked,
                    isReactionLoading = isReactionLoading,
                    onClick = {
                        viewModel.incrementViews(story.id)
                    },
                    onLikeClick = {
                        isReactionLoading = true
                        scope.launch {
                            viewModel.toggleReaction(story.id, true)
                            reaction = viewModel.getReactionCounts(story.id)
                            isUserLiked = viewModel.hasUserLiked(story.id)
                            isReactionLoading = false
                        }
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
   STORY ITEM WITH REACTIONS (for regular stories)
-------------------------------------------------- */

@Composable
fun StoryItemWithReactions(
    story: Story,
    reaction: Reaction = Reaction(),
    isUserLiked: Boolean = false,
    isReactionLoading: Boolean = false,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {}
) {
    var storyUser by remember { mutableStateOf<User?>(null) }
    var isLoadingUser by remember { mutableStateOf(false) }

    LaunchedEffect(story.userId) {
        isLoadingUser = true
        try {
            storyUser = UserRepository.getInstance().getUserById(story.userId)
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
                painter = rememberAsyncImagePainter(url),
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

        // Profile picture with border
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(40.dp)
                .border(3.dp, Color(0xFF1877F2), CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoadingUser -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1877F2)
                    )
                }
                storyUser?.avatarUrl != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(storyUser!!.avatarUrl),
                        contentDescription = "User avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default avatar",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Username at bottom
        Text(
            text = storyUser?.displayName ?: storyUser?.username ?: "User",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )

        // Reactions (likes and views)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Like button
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = !isReactionLoading) { onLikeClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isUserLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isUserLiked) "Unlike" else "Like",
                    tint = if (isUserLiked) Color.Red else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${reaction.likes}",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(4.dp))

            // View count
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveRedEye,
                    contentDescription = "Views",
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${story.viewCount}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        // Loading overlay
        if (isReactionLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}