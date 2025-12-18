package com.nidoham.socialsphere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.social.model.ReactionType
import com.nidoham.social.model.Story
import com.nidoham.social.model.StoryVisibility
import com.nidoham.social.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen
 * Manages story data, loading states, and user interactions
 */
class HomeViewModel(
    private val storyRepository: StoryRepository = StoryRepository()
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Stories list
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadStories()
    }

    /**
     * Load active stories from repository
     */
    fun loadStories(limit: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = HomeUiState.Loading

            val result = storyRepository.getActiveStories(limit)

            result.onSuccess { storiesList ->
                _stories.value = storiesList
                _uiState.value = if (storiesList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(storiesList)
                }
                _errorMessage.value = null
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to load stories"
                _uiState.value = HomeUiState.Error(exception.message ?: "Unknown error")
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh stories
     */
    fun refreshStories() {
        loadStories()
    }

    /**
     * Add reaction to a story
     */
    fun addReaction(storyId: String, reactionType: ReactionType) {
        viewModelScope.launch {
            val result = storyRepository.addReaction(storyId, reactionType)

            result.onSuccess {
                // Refresh the story to get updated stats
                refreshSingleStory(storyId)
            }.onFailure { exception ->
                _errorMessage.value = "Failed to add reaction: ${exception.message}"
            }
        }
    }

    /**
     * Remove reaction from a story
     */
    fun removeReaction(storyId: String, reactionType: ReactionType) {
        viewModelScope.launch {
            val result = storyRepository.removeReaction(storyId, reactionType)

            result.onSuccess {
                // Refresh the story to get updated stats
                refreshSingleStory(storyId)
            }.onFailure { exception ->
                _errorMessage.value = "Failed to remove reaction: ${exception.message}"
            }
        }
    }

    /**
     * Increment view count for a story
     */
    fun incrementViewCount(storyId: String) {
        viewModelScope.launch {
            storyRepository.incrementViewCount(storyId)
        }
    }

    /**
     * Increment reply count for a story
     */
    fun incrementReplyCount(storyId: String) {
        viewModelScope.launch {
            val result = storyRepository.incrementReplyCount(storyId)

            result.onSuccess {
                refreshSingleStory(storyId)
            }
        }
    }

    /**
     * Increment share count for a story
     */
    fun incrementShareCount(storyId: String) {
        viewModelScope.launch {
            val result = storyRepository.incrementShareCount(storyId)

            result.onSuccess {
                refreshSingleStory(storyId)
            }
        }
    }

    /**
     * Delete a story (soft delete)
     */
    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            val result = storyRepository.deleteStory(storyId)

            result.onSuccess {
                // Remove from local list
                _stories.value = _stories.value.filterNot { it.id == storyId }

                // Update UI state
                if (_stories.value.isEmpty()) {
                    _uiState.value = HomeUiState.Empty
                }
            }.onFailure { exception ->
                _errorMessage.value = "Failed to delete story: ${exception.message}"
            }
        }
    }

    /**
     * Get stories by visibility
     */
    fun getStoriesByVisibility(visibility: StoryVisibility, limit: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = storyRepository.getStoriesByVisibility(visibility, limit)

            result.onSuccess { storiesList ->
                _stories.value = storiesList
                _uiState.value = if (storiesList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(storiesList)
                }
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to load stories"
                _uiState.value = HomeUiState.Error(exception.message ?: "Unknown error")
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh a single story
     */
    private fun refreshSingleStory(storyId: String) {
        viewModelScope.launch {
            val result = storyRepository.getStoryById(storyId)

            result.onSuccess { updatedStory ->
                updatedStory?.let { story ->
                    _stories.value = _stories.value.map {
                        if (it.id == storyId) story else it
                    }
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Check if user can view a story
     */
    suspend fun canUserViewStory(storyId: String, userId: String): Boolean {
        val result = storyRepository.canUserViewStory(storyId, userId)
        return result.getOrDefault(false)
    }
}

/**
 * UI State sealed class for Home screen
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val stories: List<Story>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Empty : HomeUiState()
}