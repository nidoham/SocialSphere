package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nidoham.socialsphere.ui.components.PostCard
import com.nidoham.socialsphere.ui.components.PostInputPanel
import com.nidoham.socialsphere.ui.components.StorySection
import com.nidoham.socialsphere.ui.viewmodel.StoryViewModel

@Composable
fun HomeScreen(
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    val storyViewModel: StoryViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Story Section
            item {
                StorySection(
                    viewModel = storyViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            // Post Input Panel
            item {
                PostInputPanel(
                    onPostClick = onNavigateToCreatePost
                )
            }

            // Posts Feed
            items(10) { index ->
                PostCard(
                    userName = "User Name $index",
                    timeAgo = "${(1..24).random()} hours ago",
                    content = "This is a sample post content. It can contain text, images, or videos. #SocialSphere #ConnectWithWorld",
                    likeCount = "${(10..999).random()}",
                    commentCount = "${(5..99).random()}",
                    shareCount = "${(1..50).random()}",
                    onLikeClick = {
                        // TODO: Implement like functionality
                    },
                    onCommentClick = {
                        // TODO: Navigate to post detail with comments
                        onNavigateToPostDetail("post_$index")
                    },
                    onShareClick = {
                        // TODO: Implement share functionality
                    },
                    onMoreClick = {
                        // TODO: Show more options bottom sheet
                    }
                )
            }

            // Loading indicator at bottom (if needed)
            item {
                if (false) { // Replace with actual loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}