package com.nidoham.socialsphere.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.social.posts.PostExtractor
import com.nidoham.social.posts.PostWithAuthor
import com.nidoham.social.stories.StoryExtractor
import com.nidoham.social.stories.StoryWithAuthor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen
 * Manages story and post data with pagination, loading states, and user interactions
 */
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val storyExtractor = StoryExtractor(application.applicationContext)
    private val postExtractor = PostExtractor(application.applicationContext)

    // Stories State
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _stories = MutableStateFlow<List<StoryWithAuthor>>(emptyList())
    val stories: StateFlow<List<StoryWithAuthor>> = _stories.asStateFlow()

    // Posts State
    private val _postsUiState = MutableStateFlow<PostsUiState>(PostsUiState.Loading)
    val postsUiState: StateFlow<PostsUiState> = _postsUiState.asStateFlow()

    private val _posts = MutableStateFlow<List<PostWithAuthor>>(emptyList())
    val posts: StateFlow<List<PostWithAuthor>> = _posts.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Pagination state for stories
    private var currentStoryPage = 0
    private var hasMoreStoryPages = true
    private var isLoadingStoryPage = false

    // Pagination state for posts
    private var currentPostPage = 0
    private var hasMorePostPages = true
    private var isLoadingPostPage = false

    init {
        loadStories()
        loadPosts()
    }

    // ============= STORIES METHODS =============

    /**
     * Load first page of stories
     */
    fun loadStories() {
        if (isLoadingStoryPage) return

        viewModelScope.launch {
            isLoadingStoryPage = true
            _isLoading.value = true
            _uiState.value = HomeUiState.Loading

            currentStoryPage = 0
            hasMoreStoryPages = true
            storyExtractor.resetPagination()

            val result = storyExtractor.fetchStoriesPage(currentStoryPage)

            result.onSuccess { storiesList ->
                _stories.value = storiesList
                _uiState.value = if (storiesList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(storiesList)
                }
                hasMoreStoryPages = storiesList.size >= 20
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to load stories"
                _errorMessage.value = errorMsg
                _uiState.value = HomeUiState.Error(errorMsg)
                hasMoreStoryPages = false
            }

            _isLoading.value = false
            isLoadingStoryPage = false
        }
    }

    /**
     * Load next page of stories
     */
    fun loadMoreStories() {
        if (isLoadingStoryPage || !hasMoreStoryPages || _isLoadingMore.value) return

        viewModelScope.launch {
            isLoadingStoryPage = true
            _isLoadingMore.value = true

            val nextPage = currentStoryPage + 1
            val result = storyExtractor.fetchStoriesPage(nextPage)

            result.onSuccess { newStories ->
                if (newStories.isNotEmpty()) {
                    currentStoryPage = nextPage
                    _stories.value = _stories.value + newStories
                    _uiState.value = HomeUiState.Success(_stories.value)
                }
                hasMoreStoryPages = newStories.size >= 20
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load more stories: ${exception.message}"
                hasMoreStoryPages = false
            }

            _isLoadingMore.value = false
            isLoadingStoryPage = false
        }
    }

    fun refreshStories() {
        loadStories()
    }

    fun incrementViewCount(storyId: String) {
        viewModelScope.launch {
            storyExtractor.incrementViewCount(storyId)
        }
    }

    // ============= POSTS METHODS =============

    /**
     * Load first page of posts
     */
    fun loadPosts() {
        if (isLoadingPostPage) return

        viewModelScope.launch {
            isLoadingPostPage = true
            _postsUiState.value = PostsUiState.Loading

            currentPostPage = 0
            hasMorePostPages = true
            postExtractor.resetPagination()

            val result = postExtractor.fetchPostsPage(currentPostPage)

            result.onSuccess { postsList ->
                _posts.value = postsList
                _postsUiState.value = if (postsList.isEmpty()) {
                    PostsUiState.Empty
                } else {
                    PostsUiState.Success(postsList)
                }
                hasMorePostPages = postsList.size >= 20
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to load posts"
                _errorMessage.value = errorMsg
                _postsUiState.value = PostsUiState.Error(errorMsg)
                hasMorePostPages = false
            }

            isLoadingPostPage = false
        }
    }

    /**
     * Load next page of posts (Infinite Scroll)
     */
    fun loadMorePosts() {
        if (isLoadingPostPage || !hasMorePostPages || _isLoadingMorePosts.value) return

        viewModelScope.launch {
            isLoadingPostPage = true
            _isLoadingMorePosts.value = true

            val nextPage = currentPostPage + 1
            val result = postExtractor.fetchPostsPage(nextPage)

            result.onSuccess { newPosts ->
                if (newPosts.isNotEmpty()) {
                    currentPostPage = nextPage
                    _posts.value = _posts.value + newPosts
                    _postsUiState.value = PostsUiState.Success(_posts.value)
                }
                hasMorePostPages = newPosts.size >= 20
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load more posts: ${exception.message}"
                hasMorePostPages = false
            }

            _isLoadingMorePosts.value = false
            isLoadingPostPage = false
        }
    }

    fun refreshPosts() {
        loadPosts()
    }

    /**
     * Refresh both stories and posts
     */
    fun refreshAll() {
        loadStories()
        loadPosts()
    }

    /**
     * Increment post view count
     */
    fun incrementPostViewCount(postId: String) {
        viewModelScope.launch {
            postExtractor.incrementViewCount(postId)
        }
    }

    /**
     * Handle post like/unlike
     */
    fun togglePostLike(postId: String) {
        viewModelScope.launch {
            // TODO: Implement like/unlike logic with your backend
            // For now, just increment view count
            incrementPostViewCount(postId)
        }
    }

    /**
     * Delete a post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            postExtractor.removePost(postId).onSuccess {
                val updatedList = _posts.value.filterNot { it.post.id == postId }
                _posts.value = updatedList
                _postsUiState.value = if (updatedList.isEmpty()) {
                    PostsUiState.Empty
                } else {
                    PostsUiState.Success(updatedList)
                }
            }.onFailure {
                _errorMessage.value = "Delete failed: ${it.message}"
            }
        }
    }

    // ============= UTILITY METHODS =============

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearCacheAndReload() {
        viewModelScope.launch {
            storyExtractor.clearCache()
            postExtractor.clearCache()
            loadStories()
            loadPosts()
        }
    }

    fun hasMoreStories(): Boolean = hasMoreStoryPages
    fun hasMorePosts(): Boolean = hasMorePostPages
    fun getCurrentStoryPage(): Int = currentStoryPage
    fun getCurrentPostPage(): Int = currentPostPage
}

/**
 * UI State for Stories
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val stories: List<StoryWithAuthor>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data object Empty : HomeUiState()
}

/**
 * UI State for Posts
 */
sealed class PostsUiState {
    data object Loading : PostsUiState()
    data class Success(val posts: List<PostWithAuthor>) : PostsUiState()
    data class Error(val message: String) : PostsUiState()
    data object Empty : PostsUiState()
}