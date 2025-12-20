package com.nidoham.socialsphere.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.social.posts.PostWithAuthor
import com.nidoham.socialsphere.ui.item.*
import com.nidoham.socialsphere.ui.theme.DarkBackground
import com.nidoham.socialsphere.ui.viewmodel.HomeUiState
import com.nidoham.socialsphere.ui.viewmodel.HomeViewModel
import com.nidoham.socialsphere.ui.viewmodel.PostsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onStoryClick: (String) -> Unit = {}
) {
    // Collect states
    val uiState by viewModel.uiState.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val postsUiState by viewModel.postsUiState.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoadingMorePosts by viewModel.isLoadingMorePosts.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // State for liked and bookmarked posts
    var likedPosts by remember { mutableStateOf(setOf<String>()) }
    var bookmarkedPosts by remember { mutableStateOf(setOf<String>()) }

    // Loading state for SwipeRefresh
    val isRefreshing = uiState is HomeUiState.Loading && postsUiState is PostsUiState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    // LazyColumn state for pagination
    val listState = rememberLazyListState()

    // Auto-refresh when component loads
    LaunchedEffect(Unit) {
        viewModel.loadStories()
        viewModel.loadPosts()
        // TODO: Load liked and bookmarked posts from backend
        // likedPosts = viewModel.getLikedPosts()
        // bookmarkedPosts = viewModel.getBookmarkedPosts()
    }

    // Infinite scroll - load more posts when near bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= totalItems - 5) {
                    if (viewModel.hasMorePosts() && !isLoadingMorePosts) {
                        viewModel.loadMorePosts()
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshAll() }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                // ============= STORIES SECTION =============
                item(key = "stories_header") {
                    Text(
                        text = "Stories",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                item(key = "stories_list") {
                    when (uiState) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        is HomeUiState.Empty, is HomeUiState.Success -> {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 8.dp,
                                    bottom = 16.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Add Story Button
                                item(key = "add_story") {
                                    StoryItemUploader.StoryUploadButton()
                                }

                                val activeStories = stories.filter { storyWithAuthor ->
                                    storyWithAuthor.isActive() && storyWithAuthor.storyId.isNotEmpty()
                                }

                                if (activeStories.isEmpty() && uiState is HomeUiState.Success) {
                                    item(key = "no_stories") {
                                        Box(
                                            modifier = Modifier
                                                .width(200.dp)
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No active stories yet",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = activeStories,
                                        key = { it.storyId }
                                    ) { storyWithAuthor ->
                                        StoryItem(
                                            story = storyWithAuthor,
                                            onClick = { clickedStory ->
                                                if (clickedStory.storyId.isNotEmpty()) {
                                                    viewModel.incrementViewCount(clickedStory.storyId)
                                                    onStoryClick(clickedStory.storyId)
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
                                    horizontalAlignment = Alignment.CenterHorizontally
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

                // Divider
                item(key = "divider") {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Color(0xFF3A3A3A),
                        thickness = 1.dp
                    )
                }

                // ============= POSTS SECTION =============
                when (postsUiState) {
                    is PostsUiState.Loading -> {
                        item(key = "posts_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    is PostsUiState.Empty -> {
                        item(key = "posts_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No posts yet",
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Be the first to share something!",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    is PostsUiState.Success -> {
                        items(
                            items = posts,
                            key = { it.post.id }
                        ) { postWithAuthor ->
                            // Convert PostWithAuthor to PostData
                            val postData = postWithAuthor.toPostData()
                            val isLiked = likedPosts.contains(postData.postId)
                            val isBookmarked = bookmarkedPosts.contains(postData.postId)

                            SocialMediaPostItem(
                                post = postData,
                                isLiked = isLiked,
                                isBookmarked = isBookmarked,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onPostClick = { postId ->
                                    viewModel.incrementPostViewCount(postId)
                                    onPostClick(postId)
                                },
                                onLikeClick = { postId, liked ->
                                    likedPosts = if (liked) {
                                        likedPosts + postId
                                    } else {
                                        likedPosts - postId
                                    }
                                    // TODO: Update like status in backend
                                    // viewModel.toggleLike(postId, liked)
                                },
                                onCommentClick = { postId ->
                                    onCommentClick(postId)
                                },
                                onShareClick = { postId ->
                                    // TODO: Implement share functionality
                                },
                                onBookmarkClick = { postId, bookmarked ->
                                    bookmarkedPosts = if (bookmarked) {
                                        bookmarkedPosts + postId
                                    } else {
                                        bookmarkedPosts - postId
                                    }
                                    // TODO: Update bookmark status in backend
                                    // viewModel.toggleBookmark(postId, bookmarked)
                                },
                                onProfileClick = { username ->
                                    onProfileClick(username)
                                },
                                onMoreClick = { postId ->
                                    // TODO: Show more options menu (edit, delete, report)
                                },
                                onViewAllComments = { postId ->
                                    onCommentClick(postId)
                                }
                            )
                        }

                        // Loading more indicator
                        if (isLoadingMorePosts) {
                            item(key = "posts_loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // End of feed indicator
                        if (!viewModel.hasMorePosts() && posts.isNotEmpty()) {
                            item(key = "posts_end") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "You're all caught up! 🎉",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    is PostsUiState.Error -> {
                        item(key = "posts_error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = "Failed to load posts",
                                            color = Color.Red,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.refreshPosts() }) {
                                        Text("Retry", color = Color(0xFF0095F6))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom padding for navigation bar
                item(key = "bottom_padding") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
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
                containerColor = Color(0xFFFF5252),
                contentColor = Color.White
            ) {
                SelectionContainer {
                    Text(error)
                }
            }
        }
    }
}

/**
 * Extension function to convert PostWithAuthor to PostData
 */
private fun PostWithAuthor.toPostData(): PostData {
    return PostData(
        postId = this.post.id,
        authorUsername = this.author.name,
        authorAvatar = this.author.avatarUrl,
        isAuthorVerified = this.author.verified,
        timestamp = this.post.createdAt,
        privacy = PostPrivacy.PUBLIC, // TODO: Map from your Post model if it has privacy
        caption = this.post.content,
        mediaUrls = this.post.mediaUrls.map { url ->
            MediaItem(
                url = url,
                type = if (url.contains(".mp4") || url.contains(".mov")) {
                    MediaType.VIDEO
                } else {
                    MediaType.IMAGE
                }
            )
        },
        likesCount = 0, // Default to 0 as requested
        commentsCount = this.post.commentsCount,
        sharesCount = 0, // Default to 0 as requested
        topComments = emptyList() // TODO: Load top comments if available
    )
}