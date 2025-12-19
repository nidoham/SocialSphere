package com.nidoham.socialsphere.ui.viewmodel

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
import java.util.UUID

/**
 * ViewModel for uploading stories
 * Handles image upload to ImgBB and story creation in Firestore via StoryExtractor
 * FIXED: Added default constructor + initialize pattern for Compose compatibility
 */
class StoryUploadViewModel : ViewModel() {

    private val imgbbUploader: ImgbbUploader = ImgbbUploader()
    private lateinit var storyExtractor: StoryExtractor

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    /**
     * REQUIRED: Initialize the ViewModel with StoryExtractor after creation
     * Call this from your Composable: viewModel.initialize(storyExtractor)
     */
    fun initialize(storyExtractor: StoryExtractor) {
        this.storyExtractor = storyExtractor
    }

    /**
     * Upload story with image
     */
    fun uploadStory(
        userId: String,
        imageFile: File,
        caption: String,
        visibility: Story.Visibility = Story.Visibility.PUBLIC
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Step 1: Upload image to ImgBB
                val uploadResult = imgbbUploader.uploadImage(imageFile)

                if (!uploadResult.success || uploadResult.imageUrl == null) {
                    _uploadState.value = UploadState.Error(
                        uploadResult.errorMessage ?: "Failed to upload image"
                    )
                    return@launch
                }

                // Step 2: Create story object
                val story = Story(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.IMAGE.value,
                    mediaUrls = listOf(uploadResult.imageUrl),
                    visibility = visibility.value,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                // Step 3: Save story using Extractor
                val result = storyExtractor.pushStory(story)

                result.onSuccess {
                    _uploadState.value = UploadState.Success(story)
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(
                        exception.message ?: "Failed to create story"
                    )
                }

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Upload video story
     */
    fun uploadVideoStory(
        userId: String,
        videoUrl: String,
        caption: String,
        visibility: Story.Visibility = Story.Visibility.PUBLIC
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Create story object
                val story = Story(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.VIDEO.value,
                    mediaUrls = listOf(videoUrl),
                    visibility = visibility.value,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                // Save to Firestore
                val result = storyExtractor.pushStory(story)

                result.onSuccess {
                    _uploadState.value = UploadState.Success(story)
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(
                        exception.message ?: "Failed to create video story"
                    )
                }

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Upload text-only story
     */
    fun uploadTextStory(
        userId: String,
        caption: String,
        backgroundColor: String = "#4CAF50",
        visibility: Story.Visibility = Story.Visibility.PUBLIC
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Validate background color format
                if (!backgroundColor.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
                    _uploadState.value = UploadState.Error("Invalid background color format")
                    return@launch
                }

                // Create story object
                val story = Story(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.TEXT.value,
                    mediaUrls = emptyList(),
                    backgroundColor = backgroundColor,
                    visibility = visibility.value,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                // Save to Firestore
                val result = storyExtractor.pushStory(story)

                result.onSuccess {
                    _uploadState.value = UploadState.Success(story)
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(
                        exception.message ?: "Failed to create text story"
                    )
                }

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Upload story with custom metadata options
     */
    fun uploadStoryWithOptions(
        userId: String,
        imageFile: File?,
        caption: String,
        contentType: Story.ContentType = Story.ContentType.IMAGE,
        visibility: Story.Visibility = Story.Visibility.PUBLIC,
        allowedViewers: List<String> = emptyList(),
        duration: Int = 5,
        backgroundColor: String? = null,
        musicUrl: String? = null,
        location: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Upload image if provided and type is IMAGE
                val mediaUrls = if (imageFile != null && contentType == Story.ContentType.IMAGE) {
                    val uploadResult = imgbbUploader.uploadImage(imageFile)
                    if (!uploadResult.success || uploadResult.imageUrl == null) {
                        _uploadState.value = UploadState.Error(
                            uploadResult.errorMessage ?: "Failed to upload image"
                        )
                        return@launch
                    }
                    listOf(uploadResult.imageUrl)
                } else {
                    emptyList()
                }

                // Create story with all options
                val story = Story(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    caption = caption,
                    contentType = contentType.value,
                    mediaUrls = mediaUrls,
                    visibility = visibility.value,
                    allowedViewers = allowedViewers,
                    duration = duration.coerceIn(1, 60), // Enforce reasonable limits
                    backgroundColor = backgroundColor,
                    musicUrl = musicUrl,
                    location = location,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                // Save to Firestore
                val result = storyExtractor.pushStory(story)

                result.onSuccess {
                    _uploadState.value = UploadState.Success(story)
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(
                        exception.message ?: "Failed to create story"
                    )
                }

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    /**
     * Upload state sealed class
     */
    sealed class UploadState {
        object Idle : UploadState()
        object Uploading : UploadState()
        data class Success(val story: Story) : UploadState()
        data class Error(val message: String) : UploadState()
    }
}

