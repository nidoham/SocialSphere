package com.nidoham.socialsphere.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nidoham.socialsphere.ui.components.PostCard
import com.nidoham.socialsphere.ui.components.PostInputPanel
import com.nidoham.socialsphere.ui.components.StorySection
import com.nidoham.socialsphere.ui.viewmodel.PostsViewModel
import com.nidoham.socialsphere.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    val postsViewModel: PostsViewModel = viewModel()
    val storyViewModel: StoryViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val uiState = postsViewModel.uiState
    val listState = rememberLazyListState()

    // Detect when user scrolls to bottom for pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.posts.size - 3 &&
                    uiState.hasMore &&
                    !uiState.isLoading
                ) {
                    postsViewModel.loadMorePosts()
                }
            }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            postsViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isLoading && uiState.posts.isNotEmpty()),
            onRefresh = { postsViewModel.loadPosts(refresh = true) },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
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
                    // FIX 1: Removed the illegal cast `as (String) -> Unit`
                    // FIX 2: Lambda now accepts (content, imageUris)
                    PostInputPanel(
                        onPostClick = { postContent: String, imageUris: List<Uri> ->
                            // Show uploading indicator
                            if (imageUris.isNotEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Uploading images...")
                                }
                            }

                            // Create the post with content and images
                            postsViewModel.createPost(
                                content = postContent,
                                imageUris = imageUris,
                                context = context,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Post created successfully!")
                                    }
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error: $error")
                                    }
                                }
                            )
                        },
                        onPhotoClick = {
                            // Photo click handled within PostInputPanel internal launcher
                        },
                        onVideoClick = onNavigateToCreatePost
                    )
                }

                // Creating Post Indicator
                if (uiState.isCreatingPost) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Creating your post...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Initial Loading State
                if (uiState.isLoading && uiState.posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Posts Feed
                items(
                    items = uiState.posts,
                    key = { post -> post.id }
                ) { post ->
                    // FIX 3: Updated to use the new PostCard signature that takes the 'Post' object
                    PostCard(
                        post = post,
                        timeAgo = formatTimeAgo(post.createdAt),
                        onLikeClick = {
                            postsViewModel.toggleReaction(post.id, "like")
                        },
                        onCommentClick = {
                            onNavigateToPostDetail(post.id)
                        },
                        onShareClick = {
                            postsViewModel.incrementShareCount(post.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Post shared!")
                            }
                        },
                        onMoreClick = {
                            // TODO: Show more options bottom sheet
                        }
                    )
                }

                // Loading More Indicator
                if (uiState.hasMore && uiState.posts.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // No More Posts Message
                if (!uiState.hasMore && uiState.posts.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You're all caught up!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Empty State
                if (!uiState.isLoading && uiState.posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts yet. Be the first to post!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp to relative time
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        diff < 2592000000 -> "${diff / 604800000}w ago"
        else -> "${diff / 2592000000}mo ago"
    }
}