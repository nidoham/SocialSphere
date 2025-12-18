package com.nidoham.socialsphere.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.social.model.Story
import com.nidoham.socialsphere.ui.item.PostInputPanel
import com.nidoham.socialsphere.ui.item.StoryItem
import com.nidoham.socialsphere.ui.item.StoryItemUploader
import com.nidoham.socialsphere.ui.theme.DarkBackground
import com.nidoham.socialsphere.ui.viewmodel.HomeUiState
import com.nidoham.socialsphere.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    // Auto-refresh when component loads
    LaunchedEffect(Unit) {
        viewModel.loadStories()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshStories() }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {

                // Stories Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "Stories",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        // Stories Row
                        when (uiState) {
                            is HomeUiState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            is HomeUiState.Empty, is HomeUiState.Success -> {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(
                                        start = 8.dp,
                                        end = 8.dp,
                                        bottom = 8.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Add Story Item (First Item)
                                    item {
                                        StoryItemUploader.StoryUploadButton(
                                            userId = currentUserId,
                                            onStoryCreated = { newStory ->
                                                viewModel.refreshStories()
                                            }
                                        )
                                    }

                                    // Filter and display active stories
                                    val activeStories = stories.filter { story ->
                                        story.isActive() && story.id != null
                                    }

                                    if (activeStories.isEmpty() && uiState is HomeUiState.Success) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .width(200.dp)
                                                    .height(100.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No active stories",
                                                    color = Color.Gray,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    } else {
                                        items(activeStories) { story ->
                                            StoryItem(
                                                story = story,
                                                isFirstItem = false,
                                                onClick = { clickedStory ->
                                                    clickedStory.id?.let { storyId ->
                                                        viewModel.incrementViewCount(storyId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            is HomeUiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Failed to load stories",
                                            color = Color.Red,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.refreshStories() }) {
                                            Text("Retry", color = Color(0xFF0095F6))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Post Input Panel - NEW
                item {
                    PostInputPanel(
                        onPostClick = { text, imageUris ->
                            // TODO: Implement post creation logic
                            // You'll need to add a method in your ViewModel to handle post creation
                            // viewModel.createPost(text, imageUris)
                            println("Post created with text: $text and ${imageUris.size} images")
                        },
                        onPhotoClick = {
                            // Handle photo click (already handled internally by the component)
                        },
                        onVideoClick = {
                            // TODO: Implement video selection
                        },
                        onFeelingClick = {
                            // TODO: Implement feeling/activity selection
                        },
                        onMoreClick = {
                            // TODO: Implement more options menu
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                    )
                }

                // TODO: Add posts list here below stories
                // items(posts) { post ->
                //     PostItem(post)
                // }
            }
        }

        // Error Snackbar
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = Color(0xFFFF5252)
            ) {
                Text(error, color = Color.White)
            }
        }
    }
}