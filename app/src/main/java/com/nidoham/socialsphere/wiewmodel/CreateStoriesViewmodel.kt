package com.nidoham.socialsphere.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.social.stories.Story
import com.nidoham.social.stories.StoryExtractor
import com.nidoham.socialsphere.imgbb.ImgbbUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ViewModel for Create Stories Screen
 * Manages story creation state, image upload, and form validation
 */
class CreateStoriesViewModel(context: Context) : ViewModel() {

    private val imgbbUploader: ImgbbUploader = ImgbbUploader()
    private val storyExtractor: StoryExtractor = StoryExtractor(context.applicationContext)
    private val appContext = context.applicationContext

    // UI State
    private val _uiState = MutableStateFlow<CreateStoryUiState>(CreateStoryUiState.Idle)
    val uiState: StateFlow<CreateStoryUiState> = _uiState.asStateFlow()

    // Form fields
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    private val _selectedVisibility = MutableStateFlow(Story.Visibility.PUBLIC)
    val selectedVisibility: StateFlow<Story.Visibility> = _selectedVisibility.asStateFlow()

    private val _storyDuration = MutableStateFlow(5)
    val storyDuration: StateFlow<Int> = _storyDuration.asStateFlow()

    private val _enableComments = MutableStateFlow(true)
    val enableComments: StateFlow<Boolean> = _enableComments.asStateFlow()

    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location.asStateFlow()

    // Validation
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    /**
     * Update selected image URI
     */
    fun setImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        validateForm()
    }

    /**
     * Update caption text
     */
    fun setCaption(text: String) {
        _caption.value = text
        validateForm()
    }

    /**
     * Update visibility setting
     */
    fun setVisibility(visibility: Story.Visibility) {
        _selectedVisibility.value = visibility
    }

    /**
     * Update story duration (3-15 seconds)
     */
    fun setDuration(duration: Int) {
        _storyDuration.value = duration.coerceIn(3, 15)
    }

    /**
     * Toggle comments enabled
     */
    fun setEnableComments(enabled: Boolean) {
        _enableComments.value = enabled
    }

    /**
     * Set location
     */
    fun setLocation(location: String?) {
        _location.value = location
    }

    /**
     * Validate form - requires either image or caption
     */
    private fun validateForm() {
        _isFormValid.value = _selectedImageUri.value != null || _caption.value.isNotBlank()
    }

    /**
     * Upload and create story
     */
    fun createStory(userId: String) {
        if (!_isFormValid.value) {
            _uiState.value = CreateStoryUiState.Error("Please add an image or caption")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = CreateStoryUiState.Uploading(0)

                val imageUri = _selectedImageUri.value
                val mediaUrls = mutableListOf<String>()

                // Upload image if available
                if (imageUri != null) {
                    _uiState.value = CreateStoryUiState.Uploading(30)

                    val imageFile = uriToFile(imageUri)
                    if (imageFile == null) {
                        _uiState.value = CreateStoryUiState.Error("Failed to process image")
                        return@launch
                    }

                    val uploadResult = imgbbUploader.uploadImage(imageFile)

                    if (!uploadResult.success || uploadResult.imageUrl == null) {
                        _uiState.value = CreateStoryUiState.Error(
                            uploadResult.errorMessage ?: "Failed to upload image"
                        )
                        return@launch
                    }

                    mediaUrls.add(uploadResult.imageUrl)
                    _uiState.value = CreateStoryUiState.Uploading(70)
                }

                // Determine content type
                val contentType = if (mediaUrls.isNotEmpty()) {
                    Story.ContentType.IMAGE
                } else {
                    Story.ContentType.TEXT
                }

                // Create story object
                val story = Story(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    caption = _caption.value,
                    contentType = contentType.value,
                    mediaUrls = mediaUrls,
                    visibility = _selectedVisibility.value.value,
                    duration = _storyDuration.value,
                    backgroundColor = if (contentType == Story.ContentType.TEXT) "#4CAF50" else null,
                    location = _location.value,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
                    allowedViewers = emptyList()
                )

                _uiState.value = CreateStoryUiState.Uploading(90)

                // Save to Firestore
                val result = storyExtractor.pushStory(story)

                result.onSuccess {
                    _uiState.value = CreateStoryUiState.Success(story)
                    resetForm()
                }.onFailure { exception ->
                    _uiState.value = CreateStoryUiState.Error(
                        exception.message ?: "Failed to create story"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = CreateStoryUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Convert URI to File
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            val file = File(appContext.cacheDir, "story_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reset form to initial state
     */
    fun resetForm() {
        _selectedImageUri.value = null
        _caption.value = ""
        _selectedVisibility.value = Story.Visibility.PUBLIC
        _storyDuration.value = 5
        _enableComments.value = true
        _location.value = null
        _isFormValid.value = false
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is CreateStoryUiState.Error) {
            _uiState.value = CreateStoryUiState.Idle
        }
    }

    /**
     * Reset to idle state
     */
    fun resetState() {
        _uiState.value = CreateStoryUiState.Idle
    }
}

/**
 * UI State for Create Story Screen
 */
sealed class CreateStoryUiState {
    data object Idle : CreateStoryUiState()
    data class Uploading(val progress: Int) : CreateStoryUiState()
    data class Success(val story: Story) : CreateStoryUiState()
    data class Error(val message: String) : CreateStoryUiState()
}