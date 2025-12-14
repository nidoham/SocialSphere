package com.nidoham.socialsphere.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.socialsphere.ui.viewmodel.StoriesViewModel
import kotlinx.coroutines.launch

@Composable
fun StorySection(
    viewModel: StoriesViewModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showDialog by remember { mutableStateOf(false) }

    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val stories by viewModel.stories.collectAsState()

    val scope = rememberCoroutineScope()

    // Handle upload success
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Story published successfully!")
                viewModel.clearSuccess()
                showDialog = false
            }
        }
    }

    // Handle upload error
    LaunchedEffect(uploadError) {
        uploadError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar("Error: $error")
                viewModel.clearError()
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {

        Text(
            text = "Stories",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Story Button
            StoryItem(
                userName = "Your Story",
                isAddStory = true,
                onClick = { showDialog = true }
            )

            // Display user stories
            stories.forEach { story ->
                StoryItem(
                    userName = story.headline.take(10) + if (story.headline.length > 10) "..." else "",
                    isAddStory = false,
                    imageUrl = story.imageUrl,
                    onClick = {
                        // TODO: Open story viewer
                    }
                )
            }
        }
    }

    if (showDialog) {
        AddStoryDialog(
            onDismiss = {
                if (!isUploading) {
                    showDialog = false
                }
            },
            onUpload = { headline, imageFile ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                viewModel.uploadStory(headline, imageFile, userId)
            },
            isUploading = isUploading
        )
    }
}