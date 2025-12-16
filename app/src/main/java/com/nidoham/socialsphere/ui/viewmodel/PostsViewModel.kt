package com.nidoham.socialsphere.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.nidoham.socialsphere.database.cloud.model.ContentType
import com.nidoham.socialsphere.database.cloud.model.Post
import com.nidoham.socialsphere.database.cloud.model.PostVisibility
import com.nidoham.socialsphere.database.cloud.repository.PostsRepository
import com.nidoham.socialsphere.imgbb.ImgbbUploader
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * ViewModel for managing posts in the application.
 * Handles post creation, fetching, reactions, and pagination.
 */
class PostsViewModel : ViewModel() {
    private val repository = PostsRepository()
    private val auth = FirebaseAuth.getInstance()
    private val imgbbUploader = ImgbbUploader()

    // UI State
    var uiState by mutableStateOf(PostsUiState())
        private set

    // Pagination
    private var lastDocument: DocumentSnapshot? = null
    private var isLoadingMore = false

    init {
        loadPosts()
    }

    /**
     * Load initial posts or refresh
     */
    fun loadPosts(refresh: Boolean = false) {
        if (uiState.isLoading && !refresh) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, error = null)

                if (refresh) {
                    lastDocument = null
                }

                val result = repository.getPosts(
                    pageSize = 20,
                    lastDocument = if (refresh) null else lastDocument
                )

                result.onSuccess { paginatedResult ->
                    val newPosts = if (refresh) {
                        paginatedResult.data
                    } else {
                        uiState.posts + paginatedResult.data
                    }

                    lastDocument = paginatedResult.lastDocument
                    uiState = uiState.copy(
                        posts = newPosts,
                        isLoading = false,
                        hasMore = paginatedResult.hasMore
                    )
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load posts"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Load more posts (pagination)
     */
    fun loadMorePosts() {
        if (isLoadingMore || !uiState.hasMore) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val result = repository.getPosts(
                    pageSize = 20,
                    lastDocument = lastDocument
                )

                result.onSuccess { paginatedResult ->
                    lastDocument = paginatedResult.lastDocument
                    uiState = uiState.copy(
                        posts = uiState.posts + paginatedResult.data,
                        hasMore = paginatedResult.hasMore
                    )
                }
            } catch (e: Exception) {
                // Silently fail for pagination
            } finally {
                isLoadingMore = false
            }
        }
    }

    /**
     * Create a new post with optional images
     */
    fun createPost(
        content: String,
        imageUris: List<Uri> = emptyList(),
        visibility: PostVisibility = PostVisibility.PUBLIC,
        location: String? = null,
        hashtags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        context: Context? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isCreatingPost = true)

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    onError("User not logged in")
                    uiState = uiState.copy(isCreatingPost = false)
                    return@launch
                }

                // Upload images if any
                val uploadedImageUrls = mutableListOf<String>()
                imageUris.forEach { uri ->
                    try {
                        // Convert Uri to File
                        val file = if (context != null) {
                            uriToFile(context, uri)
                        } else {
                            // Fallback: try to create file from Uri path
                            uri.path?.let { File(it) }
                        }

                        if (file != null && file.exists()) {
                            val uploadResult = imgbbUploader.uploadImage(file)
                            if (uploadResult.success && uploadResult.imageUrl != null) {
                                uploadedImageUrls.add(uploadResult.imageUrl)
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with other images if one fails
                        e.printStackTrace()
                    }
                }

                // Determine content type
                val contentType = when {
                    uploadedImageUrls.isNotEmpty() -> ContentType.IMAGE
                    else -> ContentType.TEXT
                }

                // Extract links from content
                val links = extractLinks(content)

                // Create post object
                val post = Post(
                    authorId = currentUser.uid,
                    content = content,
                    contentType = contentType,
                    mediaUrls = uploadedImageUrls,
                    visibility = visibility,
                    location = location,
                    hashtags = hashtags,
                    mentions = mentions,
                    embeddedLinks = links
                )

                // Create post in repository
                val result = repository.createPost(post)

                result.onSuccess {
                    uiState = uiState.copy(isCreatingPost = false)
                    loadPosts(refresh = true) // Refresh to show new post
                    onSuccess()
                }.onFailure { exception ->
                    uiState = uiState.copy(isCreatingPost = false)
                    onError(exception.message ?: "Failed to create post")
                }

            } catch (e: Exception) {
                uiState = uiState.copy(isCreatingPost = false)
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Convert Uri to File
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Toggle reaction on a post
     */
    fun toggleReaction(postId: String, reactionType: String = "like") {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val result = repository.toggleReaction(
                    postId = postId,
                    userId = currentUser.uid,
                    reactionType = reactionType
                )

                result.onSuccess { isActive ->
                    // Update local state
                    updatePostReaction(postId, reactionType, isActive)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Update post reaction in local state
     */
    private fun updatePostReaction(postId: String, reactionType: String, isActive: Boolean) {
        val updatedPosts = uiState.posts.map { post ->
            if (post.id == postId) {
                val currentReactions = post.reactions
                val updatedReactions = when (reactionType) {
                    "like" -> currentReactions.copy(
                        likes = if (isActive) currentReactions.likes + 1 else maxOf(0, currentReactions.likes - 1)
                    )
                    "dislike" -> currentReactions.copy(
                        dislikes = if (isActive) currentReactions.dislikes + 1 else maxOf(0, currentReactions.dislikes - 1)
                    )
                    else -> currentReactions
                }
                post.copy(reactions = updatedReactions)
            } else {
                post
            }
        }
        uiState = uiState.copy(posts = updatedPosts)
    }

    /**
     * Increment comment count
     */
    fun incrementCommentCount(postId: String) {
        viewModelScope.launch {
            repository.incrementCommentCount(postId)

            // Update local state
            val updatedPosts = uiState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(commentCount = post.commentCount + 1)
                } else {
                    post
                }
            }
            uiState = uiState.copy(posts = updatedPosts)
        }
    }

    /**
     * Increment share count
     */
    fun incrementShareCount(postId: String) {
        viewModelScope.launch {
            repository.incrementShareCount(postId)

            // Update local state
            val updatedPosts = uiState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(shareCount = post.shareCount + 1)
                } else {
                    post
                }
            }
            uiState = uiState.copy(posts = updatedPosts)
        }
    }

    /**
     * Delete a post
     */
    fun deletePost(postId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = repository.deletePost(postId)

                result.onSuccess {
                    // Remove from local state
                    val updatedPosts = uiState.posts.filter { it.id != postId }
                    uiState = uiState.copy(posts = updatedPosts)
                    onSuccess()
                }.onFailure { exception ->
                    onError(exception.message ?: "Failed to delete post")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Update a post
     */
    fun updatePost(
        postId: String,
        content: String? = null,
        visibility: PostVisibility? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>()
                content?.let { updates["content"] = it }
                visibility?.let { updates["visibility"] = it.name }

                if (updates.isEmpty()) {
                    onError("No updates provided")
                    return@launch
                }

                val result = repository.updatePost(postId, updates)

                result.onSuccess {
                    // Refresh to get updated post
                    loadPosts(refresh = true)
                    onSuccess()
                }.onFailure { exception ->
                    onError(exception.message ?: "Failed to update post")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Get posts by specific user
     */
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, error = null)

                val result = repository.getPostsByUser(userId)

                result.onSuccess { paginatedResult ->
                    lastDocument = paginatedResult.lastDocument
                    uiState = uiState.copy(
                        posts = paginatedResult.data,
                        isLoading = false,
                        hasMore = paginatedResult.hasMore
                    )
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user posts"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Extract URLs from text
     */
    private fun extractLinks(text: String): List<String> {
        val urlPattern = Regex("""https?://[^\s]+""")
        return urlPattern.findAll(text).map { it.value }.toList()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

/**
 * UI State for Posts
 */
data class PostsUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingPost: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)