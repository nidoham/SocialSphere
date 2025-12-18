package com.nidoham.socialsphere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.social.model.Story
import com.nidoham.social.model.StoryMetadata
import com.nidoham.social.model.StoryVisibility
import com.nidoham.social.repository.StoryRepository
import com.nidoham.socialsphere.imgbb.ImgbbUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

/**
 * ViewModel for uploading stories
 * Handles image upload to ImgBB and story creation in Firestore
 */
class StoryUploadViewModel(
    private val imgbbUploader: ImgbbUploader = ImgbbUploader(),
    private val storyRepository: StoryRepository = StoryRepository()
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    /**
     * Upload story with image
     */
    fun uploadStory(
        userId: String,
        imageFile: File,
        caption: String,
        visibility: StoryVisibility = StoryVisibility.PUBLIC
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

                // Step 2: Create story object using factory method
                val story = Story.create(
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.IMAGE,
                    mediaUrls = listOf(uploadResult.imageUrl)
                )

                // Step 3: Set visibility in metadata
                story.metadata.setVisibilityEnum(visibility)

                // Step 4: Save story to Firestore
                val result = storyRepository.createStory(story)

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
        visibility: StoryVisibility = StoryVisibility.PUBLIC
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Create story using factory method
                val story = Story.create(
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.VIDEO,
                    mediaUrls = listOf(videoUrl)
                )

                // Set visibility
                story.metadata.setVisibilityEnum(visibility)

                // Save to Firestore
                val result = storyRepository.createStory(story)

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
        visibility: StoryVisibility = StoryVisibility.PUBLIC
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Validate background color format
                if (!backgroundColor.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
                    _uploadState.value = UploadState.Error("Invalid background color format")
                    return@launch
                }

                // Create story using factory method
                val story = Story.create(
                    authorId = userId,
                    caption = caption,
                    contentType = Story.ContentType.TEXT,
                    mediaUrls = emptyList()
                )

                // Set visibility and background color
                story.metadata.setVisibilityEnum(visibility)
                story.metadata.backgroundColor = backgroundColor

                // Save to Firestore
                val result = storyRepository.createStory(story)

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
        visibility: StoryVisibility = StoryVisibility.PUBLIC,
        allowedViewers: List<String> = emptyList(),
        duration: Int = StoryMetadata.DEFAULT_SLIDE_DURATION,
        backgroundColor: String? = null,
        musicUrl: String? = null,
        location: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading

                // Upload image if provided
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

                // Create story
                val story = Story.create(
                    authorId = userId,
                    caption = caption,
                    contentType = contentType,
                    mediaUrls = mediaUrls
                )

                // Configure metadata
                with(story.metadata) {
                    setVisibilityEnum(visibility)
                    this.allowedViewers = allowedViewers
                    this.duration = duration.coerceIn(
                        StoryMetadata.MIN_DURATION,
                        StoryMetadata.MAX_DURATION
                    )
                    this.backgroundColor = backgroundColor
                    this.musicUrl = musicUrl
                    this.location = location
                }

                // Validate before saving
                if (!story.isValid()) {
                    _uploadState.value = UploadState.Error("Invalid story data")
                    return@launch
                }

                // Save to Firestore
                val result = storyRepository.createStory(story)

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