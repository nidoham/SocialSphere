package com.nidoham.socialsphere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.socialsphere.imgbb.ImgbbUploader
import com.nidoham.socialsphere.stories.model.Story
import com.nidoham.socialsphere.stories.repository.StoriesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class StoriesViewModel(
    private val helper: StoriesHelper = StoriesHelper(),
    private val imgbbUploader: ImgbbUploader = ImgbbUploader()
) : ViewModel() {

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess = _uploadSuccess.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories = _stories.asStateFlow()

    private val _isLoadingStories = MutableStateFlow(false)
    val isLoadingStories = _isLoadingStories.asStateFlow()

    init {
        loadRecentStories()
    }

    fun uploadStory(
        headline: String,
        imageFile: File,
        userId: String
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            _uploadSuccess.value = false

            // Upload image to ImgBB
            val uploadResult = imgbbUploader.uploadImage(imageFile)

            if (!uploadResult.success || uploadResult.imageUrl == null) {
                _uploadError.value = uploadResult.errorMessage ?: "Failed to upload image"
                _isUploading.value = false
                return@launch
            }

            // Create and upload story
            val story = Story(
                storyId = "",
                userId = userId,
                headline = headline,
                imageUrl = uploadResult.imageUrl
            )

            val result = helper.uploadStory(story)

            result.fold(
                onSuccess = {
                    _uploadSuccess.value = true
                    loadRecentStories() // Refresh stories
                },
                onFailure = { e ->
                    _uploadError.value = e.localizedMessage ?: "Failed to upload story"
                }
            )

            _isUploading.value = false
        }
    }

    fun loadRecentStories() {
        viewModelScope.launch {
            _isLoadingStories.value = true

            val result = helper.fetchRecentStories()

            result.fold(
                onSuccess = { storiesList ->
                    _stories.value = storiesList
                },
                onFailure = { e ->
                    _uploadError.value = e.localizedMessage ?: "Failed to load stories"
                }
            )

            _isLoadingStories.value = false
        }
    }

    fun clearError() {
        _uploadError.value = null
    }

    fun clearSuccess() {
        _uploadSuccess.value = false
    }
}