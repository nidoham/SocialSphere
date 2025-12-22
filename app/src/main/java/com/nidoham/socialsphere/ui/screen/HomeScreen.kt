package com.nidoham.socialsphere.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nidoham.social.posts.PostWithAuthor
import com.nidoham.social.stories.StoryWithAuthor
import com.nidoham.socialsphere.extractor.ReactionCounts
import com.nidoham.socialsphere.extractor.ReactionType
import com.nidoham.socialsphere.ui.item.*
import com.nidoham.socialsphere.ui.viewmodel.HomeUiState
import com.nidoham.socialsphere.ui.viewmodel.HomeViewModel
import com.nidoham.socialsphere.ui.viewmodel.PostsUiState

/**
 * Home screen displaying stories and posts feed.
 *
 * Features:
 * - Pull to refresh
 * - Infinite scroll pagination
 * - Real-time reaction updates (Like/Love)
 * - Story viewing with view count tracking
 * - Post interactions (reactions, comment, share, bookmark)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onStoryClick: (String) -> Unit = {}
) {
    // Collect UI states
    val uiState by viewModel.uiState.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val postsUiState by viewModel.postsUiState.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoadingMorePosts by viewModel.isLoadingMorePosts.collectAsState()
    val postReactions by viewModel.postReactions.collectAsState()
    val userPostReactions by viewModel.userPostReactions.collectAsState()

    // List state for scroll position and pagination
    val listState = rememberLazyListState()

    // SwipeRefresh state
    val isRefreshing = remember(uiState, postsUiState) {
        uiState is HomeUiState.Loading && postsUiState is PostsUiState.Loading
    }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Initialize data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadStories()
        viewModel.loadPosts()
    }

    // Handle infinite scroll pagination
    InfiniteScrollHandler(
        listState = listState,
        hasMoreItems = viewModel.hasMorePosts(),
        isLoading = isLoadingMorePosts,
        onLoadMore = { viewModel.loadMorePosts() }
    )

    Box(modifier = modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshAll() }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Stories Section
                item(key = "stories_section") {
                    StoriesSection(
                        uiState = uiState,
                        stories = stories,
                        onStoryClick = { story ->
                            viewModel.incrementViewCount(story.story.id)
                            onStoryClick(story.story.id)
                        },
                        onRetry = { viewModel.refreshStories() }
                    )
                }

                // Divider
                item(key = "section_divider") {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Color(0xFF3A3A3A),
                        thickness = 1.dp
                    )
                }

                // Posts Section
                when (postsUiState) {
                    is PostsUiState.Loading -> {
                        item(key = "posts_loading") {
                            LoadingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    is PostsUiState.Empty -> {
                        item(key = "posts_empty") {
                            EmptyStateView(
                                message = "No posts yet",
                                subMessage = "Be the first to share something!",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    is PostsUiState.Success -> {
                        items(
                            items = posts,
                            key = { it.post.id }
                        ) { postWithAuthor ->
                            PostItem(
                                postWithAuthor = postWithAuthor,
                                reactionCounts = postReactions[postWithAuthor.post.id],
                                userReaction = userPostReactions[postWithAuthor.post.id],
                                onPostClick = {
                                    viewModel.incrementPostViewCount(postWithAuthor.post.id)
                                    onPostClick(postWithAuthor.post.id)
                                },
                                onReactionClick = { reaction ->
                                    viewModel.togglePostReaction(
                                        postWithAuthor.post.id,
                                        reaction
                                    )
                                },
                                onCommentClick = { onCommentClick(postWithAuthor.post.id) },
                                onShareClick = { /* TODO: Implement share */ },
                                onBookmarkClick = { /* TODO: Implement bookmark */ },
                                onProfileClick = onProfileClick,
                                onMoreClick = { /* TODO: Implement more options */ },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Loading more indicator
                        if (isLoadingMorePosts) {
                            item(key = "posts_loading_more") {
                                LoadingIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    size = 32.dp
                                )
                            }
                        }

                        // End of feed indicator
                        if (!viewModel.hasMorePosts() && posts.isNotEmpty()) {
                            item(key = "posts_end") {
                                EndOfFeedIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            }
                        }
                    }

                    is PostsUiState.Error -> {
                        item(key = "posts_error") {
                            ErrorView(
                                message = "Failed to load posts",
                                onRetry = { viewModel.refreshPosts() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Error Snackbar
        errorMessage?.let { error ->
            ErrorSnackbar(
                message = error,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

// ==================== COMPOSABLE COMPONENTS ====================

/**
 * Stories section component displaying horizontal scrollable stories
 */
@Composable
private fun StoriesSection(
    uiState: HomeUiState,
    stories: List<StoryWithAuthor>,
    onStoryClick: (StoryWithAuthor) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Text(
            text = "Stories",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        // Stories Content
        when (uiState) {
            is HomeUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }

            is HomeUiState.Empty, is HomeUiState.Success -> {
                val activeStories = remember(stories) {
                    stories.filter { it.isActive() && it.story.id.isNotEmpty() }
                }

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

                    // Active Stories
                    if (activeStories.isEmpty() && uiState is HomeUiState.Success) {
                        item(key = "no_stories") {
                            EmptyStoriesView()
                        }
                    } else {
                        items(
                            items = activeStories,
                            key = { it.story.id }
                        ) { story ->
                            StoryItem(
                                story = story,
                                onClick = { onStoryClick(story) }
                            )
                        }
                    }
                }
            }

            is HomeUiState.Error -> {
                ErrorView(
                    message = "Failed to load stories",
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Individual post item with reactions and interactions
 */
@Composable
private fun PostItem(
    postWithAuthor: PostWithAuthor,
    reactionCounts: ReactionCounts?,
    userReaction: ReactionType?,
    onPostClick: () -> Unit,
    onReactionClick: (ReactionType) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize post data to prevent unnecessary recalculations
    val postData = remember(postWithAuthor, reactionCounts) {
        postWithAuthor.toPostData(
            reactionCounts = reactionCounts ?: ReactionCounts.empty()
        )
    }

    SocialMediaPostItem(
        post = postData,
        userReaction = userReaction,
        isBookmarked = false, // TODO: Implement bookmark state
        modifier = modifier,
        onPostClick = { onPostClick() },
        onReactionClick = { _, reaction -> onReactionClick(reaction) },
        onCommentClick = { onCommentClick() },
        onShareClick = { onShareClick() },
        onBookmarkClick = { _, _ -> onBookmarkClick() },
        onProfileClick = onProfileClick,
        onMoreClick = { onMoreClick() },
        onViewAllComments = { onCommentClick() }
    )
}

// ==================== UTILITY COMPOSABLES ====================

/**
 * Handles infinite scroll pagination logic
 */
@Composable
private fun InfiniteScrollHandler(
    listState: LazyListState,
    hasMoreItems: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    threshold: Int = 5
) {
    LaunchedEffect(listState, hasMoreItems, isLoading) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null &&
                lastVisibleIndex >= totalItems - threshold &&
                hasMoreItems &&
                !isLoading
            ) {
                onLoadMore()
            }
        }
    }
}

/**
 * Loading indicator component
 */
@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF4CAF50),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Empty state view
 */
@Composable
private fun EmptyStateView(
    message: String,
    subMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                color = Color.Gray,
                fontSize = 16.sp
            )
            subMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Error view with retry button
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                color = Color.Red,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    color = Color(0xFF0095F6)
                )
            }
        }
    }
}

/**
 * Empty stories placeholder
 */
@Composable
private fun EmptyStoriesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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

/**
 * End of feed indicator
 */
@Composable
private fun EndOfFeedIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "You're all caught up! 🎉",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

/**
 * Error snackbar component
 */
@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        action = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFFFF5252),
        contentColor = Color.White
    ) {
        Text(text = message)
    }
}

// ==================== EXTENSION FUNCTIONS ====================

/**
 * Converts PostWithAuthor to PostData with reaction counts.
 * Extracted as extension for better organization and reusability.
 */
private fun PostWithAuthor.toPostData(
    reactionCounts: ReactionCounts
): PostData {
    return PostData(
        postId = this.post.id,
        authorUsername = this.author.name,
        authorAvatar = this.author.avatarUrl,
        isAuthorVerified = this.author.verified,
        timestamp = this.post.createdAt,
        privacy = PostPrivacy.PUBLIC,
        caption = this.post.content,
        mediaUrls = this.post.mediaUrls.map { url ->
            MediaItem(
                url = url,
                type = inferMediaType(url)
            )
        },
        reactionCounts = reactionCounts,
        commentsCount = this.post.commentsCount,
        sharesCount = 0, // TODO: Implement shares tracking
        topComments = emptyList(), // TODO: Load top comments if available
        likedByUsers = emptyList() // TODO: Load users who liked if available
    )
}

/**
 * Infers media type from URL.
 */
private fun inferMediaType(url: String): MediaType {
    return when {
        url.contains(".mp4", ignoreCase = true) ||
                url.contains(".mov", ignoreCase = true) ||
                url.contains(".avi", ignoreCase = true) ||
                url.contains(".webm", ignoreCase = true) -> MediaType.VIDEO
        else -> MediaType.IMAGE
    }
}