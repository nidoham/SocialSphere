package com.nidoham.socialsphere.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.socialsphere.database.cloud.model.Reaction
import com.nidoham.socialsphere.database.cloud.model.Story
import com.nidoham.socialsphere.database.cloud.repository.StoryRepository
import com.nidoham.socialsphere.imgbb.ImgbbUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * StoryViewModel - Production-Ready Story Management
 */
class StoryViewModel(
    private val storyRepository: StoryRepository = StoryRepository.getInstance(),
    private val imgbbUploader: ImgbbUploader = ImgbbUploader()
) : ViewModel() {

    companion object {
        private const val TAG = "StoryViewModel"
    }

    // Upload states
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess = _uploadSuccess.asStateFlow()

    // Stories list states
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories = _stories.asStateFlow()

    private val _isLoadingStories = MutableStateFlow(false)
    val isLoadingStories = _isLoadingStories.asStateFlow()

    private val _loadStoriesError = MutableStateFlow<String?>(null)
    val loadStoriesError = _loadStoriesError.asStateFlow()

    // Reaction states
    private val _reactionLoading = MutableStateFlow<String?>(null)
    val reactionLoading = _reactionLoading.asStateFlow()

    private val _reactionError = MutableStateFlow<String?>(null)
    val reactionError = _reactionError.asStateFlow()

    init {
        loadRecentStories()
    }

    // ==================== UPLOAD STORY ====================

    /**
     * Uploads a new story with image
     * @param caption Story caption/headline
     * @param imageFile Image file to upload
     * @param userId User ID of story creator
     */
    fun uploadStory(caption: String, imageFile: File, userId: String) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            _uploadSuccess.value = false

            try {
                // Upload image to ImgBB
                val uploadResult = imgbbUploader.uploadImage(imageFile)
                if (!uploadResult.success || uploadResult.imageUrl == null) {
                    _uploadError.value = uploadResult.errorMessage ?: "Failed to upload image"
                    Log.e(TAG, "Image upload failed: ${uploadResult.errorMessage}")
                    return@launch
                }

                // Create story object
                val now = System.currentTimeMillis()
                val expiresAt = Story.getExpiryTimestamp(now)

                val story = Story(
                    id = "", // Will be set by Firestore
                    userId = userId,
                    caption = caption,
                    imageUrl = uploadResult.imageUrl,
                    videoUrl = null,
                    createdAt = now,
                    expiresAt = expiresAt,
                    isBanned = false,
                    viewCount = 0L,
                    replyCount = 0L,
                    shareCount = 0L,
                    reactions = Reaction(),
                    visibility = Story.VISIBILITY_PUBLIC,
                    allowedViewers = emptyList()
                )

                // Save story to Firestore
                val result = storyRepository.createStory(story)
                result.fold(
                    onSuccess = { storyId ->
                        _uploadSuccess.value = true
                        Log.d(TAG, "Story uploaded successfully: $storyId")
                        loadRecentStories() // Refresh stories
                    },
                    onFailure = { e ->
                        _uploadError.value = e.localizedMessage ?: "Failed to upload story"
                        Log.e(TAG, "Story upload failed", e)
                    }
                )
            } catch (e: Exception) {
                _uploadError.value = e.localizedMessage ?: "Unexpected error occurred"
                Log.e(TAG, "Unexpected error during story upload", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    // ==================== LOAD STORIES ====================

    /**
     * Loads recent active stories (within 24 hours)
     */
    fun loadRecentStories() {
        viewModelScope.launch {
            _isLoadingStories.value = true
            _loadStoriesError.value = null

            try {
                val result = storyRepository.getRecentStories(20)
                result.fold(
                    onSuccess = { storiesList ->
                        _stories.value = storiesList
                        Log.d(TAG, "Loaded ${storiesList.size} stories")
                    },
                    onFailure = { e ->
                        _loadStoriesError.value = e.localizedMessage ?: "Failed to load stories"
                        Log.e(TAG, "Failed to load stories", e)
                    }
                )
            } catch (e: Exception) {
                _loadStoriesError.value = e.localizedMessage ?: "Failed to load stories"
                Log.e(TAG, "Unexpected error loading stories", e)
            } finally {
                _isLoadingStories.value = false
            }
        }
    }

    /**
     * Loads stories for a specific user
     * @param userId User ID
     */
    fun loadUserStories(userId: String) {
        viewModelScope.launch {
            _isLoadingStories.value = true
            _loadStoriesError.value = null

            try {
                val result = storyRepository.getUserStories(userId, 20)
                result.fold(
                    onSuccess = { storiesList ->
                        _stories.value = storiesList
                        Log.d(TAG, "Loaded ${storiesList.size} user stories")
                    },
                    onFailure = { e ->
                        _loadStoriesError.value = e.localizedMessage ?: "Failed to load user stories"
                        Log.e(TAG, "Failed to load user stories", e)
                    }
                )
            } catch (e: Exception) {
                _loadStoriesError.value = e.localizedMessage ?: "Failed to load user stories"
                Log.e(TAG, "Unexpected error loading user stories", e)
            } finally {
                _isLoadingStories.value = false
            }
        }
    }

    /**
     * Refreshes stories (for pull-to-refresh)
     */
    fun refreshStories() {
        loadRecentStories()
    }

    // ==================== REACTIONS ====================

    /**
     * Toggles like/dislike reaction on a story
     * @param storyId Story ID
     * @param isLike True for like, false for dislike
     */
    fun toggleReaction(storyId: String, isLike: Boolean = true) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _reactionError.value = "User not authenticated"
                return@launch
            }

            _reactionLoading.value = storyId
            _reactionError.value = null

            try {
                val result = storyRepository.toggleReaction(storyId, userId, isLike)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Reaction toggled successfully")
                        // Optionally refresh stories to get updated counts
                        // loadRecentStories()
                    },
                    onFailure = { e ->
                        _reactionError.value = e.localizedMessage ?: "Failed to toggle reaction"
                        Log.e(TAG, "Failed to toggle reaction", e)
                    }
                )
            } catch (e: Exception) {
                _reactionError.value = e.localizedMessage ?: "Failed to toggle reaction"
                Log.e(TAG, "Unexpected error toggling reaction", e)
            } finally {
                _reactionLoading.value = null
            }
        }
    }

    /**
     * Gets reaction counts for a specific story
     * @param storyId Story ID
     * @return Reaction object with counts
     */
    suspend fun getReactionCounts(storyId: String): Reaction {
        return try {
            storyRepository.getReactionCounts(storyId).getOrNull() ?: Reaction()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reaction counts", e)
            Reaction()
        }
    }

    /**
     * Checks if current user has liked a story
     * @param storyId Story ID
     * @return True if user has liked the story
     */
    suspend fun hasUserLiked(storyId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            storyRepository.hasUserLiked(storyId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check like status", e)
            false
        }
    }

    // ==================== VIEW COUNT ====================

    /**
     * Increments view count for a story
     * @param storyId Story ID
     */
    fun incrementViews(storyId: String) {
        viewModelScope.launch {
            try {
                storyRepository.incrementViewCount(storyId)
                Log.d(TAG, "View count incremented for story: $storyId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to increment view count", e)
            }
        }
    }

    /**
     * Batch increments view count for multiple stories
     * @param storyIds List of story IDs
     */
    fun batchIncrementViews(storyIds: List<String>) {
        viewModelScope.launch {
            try {
                storyRepository.batchIncrementViews(storyIds)
                Log.d(TAG, "Batch view count updated for ${storyIds.size} stories")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to batch increment views", e)
            }
        }
    }

    // ==================== DELETE STORY ====================

    /**
     * Deletes a story
     * @param storyId Story ID
     */
    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            try {
                val result = storyRepository.deleteStory(storyId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Story deleted successfully")
                        loadRecentStories() // Refresh stories
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to delete story", e)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error deleting story", e)
            }
        }
    }

    // ==================== ERROR CLEARING ====================

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun clearLoadError() {
        _loadStoriesError.value = null
    }

    fun clearReactionError() {
        _reactionError.value = null
    }

    fun clearSuccess() {
        _uploadSuccess.value = false
    }

    fun clearAllErrors() {
        _uploadError.value = null
        _loadStoriesError.value = null
        _reactionError.value = null
    }
}