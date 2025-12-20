package com.nidoham.socialsphere.ui.viewmodel

import android.app.Application
import android.util.Log
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

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 20
    }

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
        Log.d(TAG, "HomeViewModel initialized")
        // Don't auto-load here, let the UI trigger it
    }

    // ============= STORIES METHODS =============

    /**
     * Load first page of stories
     */
    fun loadStories() {
        if (isLoadingStoryPage) {
            Log.d(TAG, "Already loading stories, skipping...")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingStoryPage = true
                _isLoading.value = true
                _uiState.value = HomeUiState.Loading

                Log.d(TAG, "Loading stories page 0...")
                currentStoryPage = 0
                hasMoreStoryPages = true
                storyExtractor.resetPagination()

                val result = storyExtractor.fetchStoriesPage(currentStoryPage)

                result.onSuccess { storiesList ->
                    Log.d(TAG, "Successfully loaded ${storiesList.size} stories")
                    _stories.value = storiesList
                    _uiState.value = if (storiesList.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(storiesList)
                    }
                    hasMoreStoryPages = storiesList.size >= PAGE_SIZE
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to load stories"
                    Log.e(TAG, "Failed to load stories: $errorMsg", exception)
                    _errorMessage.value = errorMsg
                    _uiState.value = HomeUiState.Error(errorMsg)
                    hasMoreStoryPages = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading stories", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
                isLoadingStoryPage = false
            }
        }
    }

    /**
     * Load next page of stories
     */
    fun loadMoreStories() {
        if (isLoadingStoryPage || !hasMoreStoryPages || _isLoadingMore.value) {
            Log.d(TAG, "Skipping load more stories - loading: $isLoadingStoryPage, hasMore: $hasMoreStoryPages")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingStoryPage = true
                _isLoadingMore.value = true

                val nextPage = currentStoryPage + 1
                Log.d(TAG, "Loading stories page $nextPage...")
                val result = storyExtractor.fetchStoriesPage(nextPage)

                result.onSuccess { newStories ->
                    Log.d(TAG, "Loaded ${newStories.size} more stories")
                    if (newStories.isNotEmpty()) {
                        currentStoryPage = nextPage
                        _stories.value = _stories.value + newStories
                        _uiState.value = HomeUiState.Success(_stories.value)
                    }
                    hasMoreStoryPages = newStories.size >= PAGE_SIZE
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to load more stories", exception)
                    _errorMessage.value = "Failed to load more stories: ${exception.message}"
                    hasMoreStoryPages = false
                }
            } finally {
                _isLoadingMore.value = false
                isLoadingStoryPage = false
            }
        }
    }

    fun refreshStories() {
        Log.d(TAG, "Refreshing stories...")
        loadStories()
    }

    fun incrementViewCount(storyId: String) {
        viewModelScope.launch {
            try {
                storyExtractor.incrementViewCount(storyId)
                Log.d(TAG, "Incremented view count for story: $storyId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to increment view count", e)
            }
        }
    }

    // ============= POSTS METHODS =============

    /**
     * Load first page of posts
     */
    fun loadPosts() {
        if (isLoadingPostPage) {
            Log.d(TAG, "Already loading posts, skipping...")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingPostPage = true
                _postsUiState.value = PostsUiState.Loading

                Log.d(TAG, "Loading posts page 0...")
                currentPostPage = 0
                hasMorePostPages = true
                postExtractor.resetPagination()

                val result = postExtractor.fetchPostsPage(currentPostPage)

                result.onSuccess { postsList ->
                    Log.d(TAG, "Successfully loaded ${postsList.size} posts")
                    _posts.value = postsList
                    _postsUiState.value = if (postsList.isEmpty()) {
                        PostsUiState.Empty
                    } else {
                        PostsUiState.Success(postsList)
                    }
                    hasMorePostPages = postsList.size >= PAGE_SIZE
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to load posts"
                    Log.e(TAG, "Failed to load posts: $errorMsg", exception)
                    _errorMessage.value = errorMsg
                    _postsUiState.value = PostsUiState.Error(errorMsg)
                    hasMorePostPages = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading posts", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
                _postsUiState.value = PostsUiState.Error(e.message ?: "Unknown error")
            } finally {
                isLoadingPostPage = false
            }
        }
    }

    /**
     * Load next page of posts (Infinite Scroll)
     */
    fun loadMorePosts() {
        if (isLoadingPostPage || !hasMorePostPages || _isLoadingMorePosts.value) {
            Log.d(TAG, "Skipping load more posts - loading: $isLoadingPostPage, hasMore: $hasMorePostPages")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingPostPage = true
                _isLoadingMorePosts.value = true

                val nextPage = currentPostPage + 1
                Log.d(TAG, "Loading posts page $nextPage...")
                val result = postExtractor.fetchPostsPage(nextPage)

                result.onSuccess { newPosts ->
                    Log.d(TAG, "Loaded ${newPosts.size} more posts")
                    if (newPosts.isNotEmpty()) {
                        currentPostPage = nextPage
                        _posts.value = _posts.value + newPosts
                        _postsUiState.value = PostsUiState.Success(_posts.value)
                    }
                    hasMorePostPages = newPosts.size >= PAGE_SIZE
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to load more posts", exception)
                    _errorMessage.value = "Failed to load more posts: ${exception.message}"
                    hasMorePostPages = false
                }
            } finally {
                _isLoadingMorePosts.value = false
                isLoadingPostPage = false
            }
        }
    }

    fun refreshPosts() {
        Log.d(TAG, "Refreshing posts...")
        loadPosts()
    }

    /**
     * Refresh both stories and posts
     */
    fun refreshAll() {
        Log.d(TAG, "Refreshing all content...")
        loadStories()
        loadPosts()
    }

    /**
     * Increment post view count
     */
    fun incrementPostViewCount(postId: String) {
        viewModelScope.launch {
            try {
                postExtractor.incrementViewCount(postId)
                Log.d(TAG, "Incremented view count for post: $postId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to increment post view count", e)
            }
        }
    }

    /**
     * Delete a post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting post: $postId")
                postExtractor.removePost(postId).onSuccess {
                    val updatedList = _posts.value.filterNot { it.post.id == postId }
                    _posts.value = updatedList
                    _postsUiState.value = if (updatedList.isEmpty()) {
                        PostsUiState.Empty
                    } else {
                        PostsUiState.Success(updatedList)
                    }
                    Log.d(TAG, "Successfully deleted post")
                }.onFailure {
                    Log.e(TAG, "Failed to delete post", it)
                    _errorMessage.value = "Delete failed: ${it.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error deleting post", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
            }
        }
    }

    // ============= UTILITY METHODS =============

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearCacheAndReload() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing cache and reloading...")
                storyExtractor.clearCache()
                postExtractor.clearCache()
                loadStories()
                loadPosts()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
                _errorMessage.value = "Failed to clear cache: ${e.message}"
            }
        }
    }

    fun hasMoreStories(): Boolean = hasMoreStoryPages
    fun hasMorePosts(): Boolean = hasMorePostPages
    fun getCurrentStoryPage(): Int = currentStoryPage
    fun getCurrentPostPage(): Int = currentPostPage

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HomeViewModel cleared")
    }
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