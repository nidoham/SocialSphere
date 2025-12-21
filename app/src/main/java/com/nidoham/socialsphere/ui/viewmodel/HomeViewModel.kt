package com.nidoham.socialsphere.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nidoham.social.posts.PostExtractor
import com.nidoham.social.posts.PostWithAuthor
import com.nidoham.social.stories.StoryExtractor
import com.nidoham.social.stories.StoryWithAuthor
import com.nidoham.socialsphere.extractor.ContentType
import com.nidoham.socialsphere.extractor.ReactionCounts
import com.nidoham.socialsphere.extractor.ReactionManager
import com.nidoham.socialsphere.extractor.ReactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Manages story and post data with pagination, loading states, reactions, and user interactions.
 * Implements proper separation of concerns and follows MVVM architecture.
 */
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 20
        private const val LOAD_MORE_THRESHOLD = 5
    }

    // Dependencies
    private val storyExtractor = StoryExtractor(application.applicationContext)
    private val postExtractor = PostExtractor(application.applicationContext)
    private val reactionManager = ReactionManager(Firebase.firestore)
    private val auth = FirebaseAuth.getInstance()

    // Current user ID
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== STORIES STATE ====================

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _stories = MutableStateFlow<List<StoryWithAuthor>>(emptyList())
    val stories: StateFlow<List<StoryWithAuthor>> = _stories.asStateFlow()

    // ==================== POSTS STATE ====================

    private val _postsUiState = MutableStateFlow<PostsUiState>(PostsUiState.Loading)
    val postsUiState: StateFlow<PostsUiState> = _postsUiState.asStateFlow()

    private val _posts = MutableStateFlow<List<PostWithAuthor>>(emptyList())
    val posts: StateFlow<List<PostWithAuthor>> = _posts.asStateFlow()

    // ==================== REACTIONS STATE ====================

    private val _postReactions = MutableStateFlow<Map<String, ReactionCounts>>(emptyMap())
    val postReactions: StateFlow<Map<String, ReactionCounts>> = _postReactions.asStateFlow()

    private val _storyReactions = MutableStateFlow<Map<String, ReactionCounts>>(emptyMap())
    val storyReactions: StateFlow<Map<String, ReactionCounts>> = _storyReactions.asStateFlow()

    private val _userPostReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val userPostReactions: StateFlow<Map<String, ReactionType>> = _userPostReactions.asStateFlow()

    private val _userStoryReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val userStoryReactions: StateFlow<Map<String, ReactionType>> = _userStoryReactions.asStateFlow()

    // ==================== LOADING STATE ====================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    // ==================== ERROR STATE ====================

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== PAGINATION STATE ====================

    // Story pagination
    private var currentStoryPage = 0
    private var hasMoreStoryPages = true
    private var isLoadingStoryPage = false

    // Post pagination
    private var currentPostPage = 0
    private var hasMorePostPages = true
    private var isLoadingPostPage = false

    init {
        Log.d(TAG, "HomeViewModel initialized")
    }

    // ==================== STORIES METHODS ====================

    /**
     * Load first page of stories with reactions.
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

                    // Load reactions for stories
                    loadStoryReactions(storiesList.map { it.story.id })
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
     * Load next page of stories with reactions.
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

                        // Load reactions for new stories
                        loadStoryReactions(newStories.map { it.story.id })
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

    /**
     * Refresh stories from the beginning.
     */
    fun refreshStories() {
        Log.d(TAG, "Refreshing stories...")
        loadStories()
    }

    /**
     * Increment view count for a story.
     */
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

    // ==================== POSTS METHODS ====================

    /**
     * Load first page of posts with reactions.
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

                    // Load reactions for posts
                    loadPostReactions(postsList.map { it.post.id })
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
     * Load next page of posts with reactions (Infinite Scroll).
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

                        // Load reactions for new posts
                        loadPostReactions(newPosts.map { it.post.id })
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

    /**
     * Refresh posts from the beginning.
     */
    fun refreshPosts() {
        Log.d(TAG, "Refreshing posts...")
        loadPosts()
    }

    /**
     * Refresh both stories and posts.
     */
    fun refreshAll() {
        Log.d(TAG, "Refreshing all content...")
        loadStories()
        loadPosts()
    }

    /**
     * Increment post view count.
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
     * Delete a post and update UI state.
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

                    // Remove reaction data for deleted post
                    _postReactions.value = _postReactions.value - postId
                    _userPostReactions.value = _userPostReactions.value - postId

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

    // ==================== REACTIONS METHODS ====================

    /**
     * Toggle reaction on a post (like or love).
     * Optimistically updates UI before server response.
     */
    fun togglePostReaction(postId: String, reaction: ReactionType) {
        val userId = currentUserId
        if (userId == null) {
            Log.w(TAG, "Cannot toggle reaction: User not logged in")
            _errorMessage.value = "Please log in to react"
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Toggling ${reaction.value} on post $postId")

                reactionManager.toggleReaction(
                    contentType = ContentType.Post,
                    contentId = postId,
                    userId = userId,
                    reaction = reaction
                ).onSuccess { updatedCounts ->
                    // Update reaction counts
                    _postReactions.value = _postReactions.value + (postId to updatedCounts)

                    // Update user's reaction state
                    val currentReaction = _userPostReactions.value[postId]
                    if (currentReaction == reaction) {
                        // User removed their reaction
                        _userPostReactions.value = _userPostReactions.value - postId
                    } else {
                        // User added or changed reaction
                        _userPostReactions.value = _userPostReactions.value + (postId to reaction)
                    }

                    Log.d(TAG, "Post reaction updated: ${updatedCounts.likes} likes, ${updatedCounts.loves} loves")
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to toggle post reaction", exception)
                    _errorMessage.value = "Failed to react: ${exception.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error toggling post reaction", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
            }
        }
    }

    /**
     * Toggle reaction on a story (like or love).
     */
    fun toggleStoryReaction(storyId: String, reaction: ReactionType) {
        val userId = currentUserId
        if (userId == null) {
            Log.w(TAG, "Cannot toggle reaction: User not logged in")
            _errorMessage.value = "Please log in to react"
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Toggling ${reaction.value} on story $storyId")

                reactionManager.toggleReaction(
                    contentType = ContentType.Story,
                    contentId = storyId,
                    userId = userId,
                    reaction = reaction
                ).onSuccess { updatedCounts ->
                    // Update reaction counts
                    _storyReactions.value = _storyReactions.value + (storyId to updatedCounts)

                    // Update user's reaction state
                    val currentReaction = _userStoryReactions.value[storyId]
                    if (currentReaction == reaction) {
                        // User removed their reaction
                        _userStoryReactions.value = _userStoryReactions.value - storyId
                    } else {
                        // User added or changed reaction
                        _userStoryReactions.value = _userStoryReactions.value + (storyId to reaction)
                    }

                    Log.d(TAG, "Story reaction updated: ${updatedCounts.likes} likes, ${updatedCounts.loves} loves")
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to toggle story reaction", exception)
                    _errorMessage.value = "Failed to react: ${exception.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error toggling story reaction", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
            }
        }
    }

    /**
     * Load reaction data for multiple posts.
     * Uses batch operations for efficiency.
     */
    private fun loadPostReactions(postIds: List<String>) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Load reaction counts for all posts
                reactionManager.getBatchReactionCounts(
                    contentType = ContentType.Post,
                    contentIds = postIds
                ).onSuccess { countsMap ->
                    _postReactions.value = _postReactions.value + countsMap
                }

                // Load user's reactions using batch operation
                reactionManager.getBatchUserReactions(
                    contentIds = postIds,
                    userId = userId
                ).onSuccess { reactionsMap ->
                    val filtered = reactionsMap.filterValues { it != null }
                        .mapValues { it.value!! }
                    _userPostReactions.value = _userPostReactions.value + filtered
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load post reactions", e)
            }
        }
    }

    /**
     * Load reaction data for multiple stories.
     * Uses batch operations for efficiency.
     */
    private fun loadStoryReactions(storyIds: List<String>) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Load reaction counts for all stories
                reactionManager.getBatchReactionCounts(
                    contentType = ContentType.Story,
                    contentIds = storyIds
                ).onSuccess { countsMap ->
                    _storyReactions.value = _storyReactions.value + countsMap
                }

                // Load user's reactions using batch operation
                reactionManager.getBatchUserReactions(
                    contentIds = storyIds,
                    userId = userId
                ).onSuccess { reactionsMap ->
                    val filtered = reactionsMap.filterValues { it != null }
                        .mapValues { it.value!! }
                    _userStoryReactions.value = _userStoryReactions.value + filtered
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load story reactions", e)
            }
        }
    }

    // ==================== GETTER METHODS ====================

    /**
     * Get reaction counts for a specific post.
     */
    fun getPostReactionCounts(postId: String): ReactionCounts? {
        return _postReactions.value[postId]
    }

    /**
     * Get reaction counts for a specific story.
     */
    fun getStoryReactionCounts(storyId: String): ReactionCounts? {
        return _storyReactions.value[storyId]
    }

    /**
     * Get user's reaction for a specific post.
     */
    fun getUserPostReaction(postId: String): ReactionType? {
        return _userPostReactions.value[postId]
    }

    /**
     * Get user's reaction for a specific story.
     */
    fun getUserStoryReaction(storyId: String): ReactionType? {
        return _userStoryReactions.value[storyId]
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear all caches and reload data.
     */
    fun clearCacheAndReload() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing cache and reloading...")
                storyExtractor.clearCache()
                postExtractor.clearCache()

                // Clear reaction caches
                _postReactions.value = emptyMap()
                _storyReactions.value = emptyMap()
                _userPostReactions.value = emptyMap()
                _userStoryReactions.value = emptyMap()

                loadStories()
                loadPosts()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
                _errorMessage.value = "Failed to clear cache: ${e.message}"
            }
        }
    }

    /**
     * Check if there are more stories to load.
     */
    fun hasMoreStories(): Boolean = hasMoreStoryPages

    /**
     * Check if there are more posts to load.
     */
    fun hasMorePosts(): Boolean = hasMorePostPages

    /**
     * Get current story page number.
     */
    fun getCurrentStoryPage(): Int = currentStoryPage

    /**
     * Get current post page number.
     */
    fun getCurrentPostPage(): Int = currentPostPage

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HomeViewModel cleared")
    }
}

// ==================== UI STATE CLASSES ====================

/**
 * UI State for Stories section.
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val stories: List<StoryWithAuthor>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data object Empty : HomeUiState()
}

/**
 * UI State for Posts section.
 */
sealed class PostsUiState {
    data object Loading : PostsUiState()
    data class Success(val posts: List<PostWithAuthor>) : PostsUiState()
    data class Error(val message: String) : PostsUiState()
    data object Empty : PostsUiState()
}