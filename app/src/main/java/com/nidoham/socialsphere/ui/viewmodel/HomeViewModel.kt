package com.nidoham.socialsphere.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.social.stories.StoryExtractor
import com.nidoham.social.stories.StoryWithAuthor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen
 * Manages story data with pagination, loading states, and user interactions
 */
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Context-এর পরিবর্তে Application Context ব্যবহার করা নিরাপদ (Memory Leak রোধ করতে)
    private val storyExtractor = StoryExtractor(application.applicationContext)

    // UI State
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Stories list
    private val _stories = MutableStateFlow<List<StoryWithAuthor>>(emptyList())
    val stories: StateFlow<List<StoryWithAuthor>> = _stories.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Pagination state
    private var currentPage = 0
    private var hasMorePages = true
    private var isLoadingPage = false

    init {
        loadStories()
    }

    /**
     * Load first page of stories
     */
    fun loadStories() {
        if (isLoadingPage) return

        viewModelScope.launch {
            isLoadingPage = true
            _isLoading.value = true
            _uiState.value = HomeUiState.Loading

            // Reset pagination
            currentPage = 0
            hasMorePages = true
            storyExtractor.resetPagination()

            val result = storyExtractor.fetchStoriesPage(currentPage)

            result.onSuccess { storiesList ->
                _stories.value = storiesList
                _uiState.value = if (storiesList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(storiesList)
                }
                _errorMessage.value = null
                hasMorePages = storiesList.size >= 20
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to load stories"
                _errorMessage.value = errorMsg
                _uiState.value = HomeUiState.Error(errorMsg)
                hasMorePages = false
            }

            _isLoading.value = false
            isLoadingPage = false
        }
    }

    /**
     * Load next page of stories (Infinite Scroll)
     */
    fun loadMoreStories() {
        if (isLoadingPage || !hasMorePages || _isLoadingMore.value) return

        viewModelScope.launch {
            isLoadingPage = true
            _isLoadingMore.value = true

            val nextPage = currentPage + 1
            val result = storyExtractor.fetchStoriesPage(nextPage)

            result.onSuccess { newStories ->
                if (newStories.isNotEmpty()) {
                    currentPage = nextPage
                    _stories.value = _stories.value + newStories

                    // Update Success state with updated list
                    _uiState.value = HomeUiState.Success(_stories.value)
                }
                hasMorePages = newStories.size >= 20
                _errorMessage.value = null
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load more: ${exception.message}"
                hasMorePages = false
            }

            _isLoadingMore.value = false
            isLoadingPage = false
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

    fun loadStoriesByAuthor(authorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = HomeUiState.Loading

            val result = storyExtractor.fetchStoriesByAuthor(authorId)

            result.onSuccess { storiesList ->
                _stories.value = storiesList
                _uiState.value = if (storiesList.isEmpty()) HomeUiState.Empty else HomeUiState.Success(storiesList)
                _errorMessage.value = null
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to load author stories"
                _errorMessage.value = errorMsg
                _uiState.value = HomeUiState.Error(errorMsg)
            }
            _isLoading.value = false
        }
    }

    fun loadStoryWithAuthor(storyId: String) {
        viewModelScope.launch {
            val result = storyExtractor.fetchStoryWithAuthor(storyId)
            result.onSuccess { storyWithAuthor ->
                val currentList = _stories.value.toMutableList()
                val index = currentList.indexOfFirst { it.storyId == storyId }

                if (index >= 0) {
                    currentList[index] = storyWithAuthor
                } else {
                    currentList.add(storyWithAuthor)
                }
                _stories.value = currentList
                _uiState.value = HomeUiState.Success(currentList)
            }
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            storyExtractor.removeStory(storyId).onSuccess {
                val updatedList = _stories.value.filterNot { it.storyId == storyId }
                _stories.value = updatedList
                _uiState.value = if (updatedList.isEmpty()) HomeUiState.Empty else HomeUiState.Success(updatedList)
            }.onFailure {
                _errorMessage.value = "Delete failed: ${it.message}"
            }
        }
    }

    fun clearCacheAndReload() {
        viewModelScope.launch {
            storyExtractor.clearCache()
            loadStories()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun hasMoreStories(): Boolean = hasMorePages
    fun getCurrentPage(): Int = currentPage
}

/**
 * UI State sealed class for Home screen
 * 'data object' is preferred in modern Kotlin for singleton states
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val stories: List<StoryWithAuthor>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data object Empty : HomeUiState()
}