package com.nidoham.socialsphere.ui.item

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.social.stories.Story
import com.nidoham.socialsphere.ui.viewmodel.StoryUploadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Instagram-style Story uploader with visibility controls and polished UI/UX
 */
object StoryItemUploader {

    @Composable
    fun StoryUploadButton(
        userId: String,
        onStoryCreated: (Story) -> Unit,
        modifier: Modifier = Modifier,
        uploadViewModel: StoryUploadViewModel = viewModel()
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var showCaptionDialog by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }
        var showError by remember { mutableStateOf<String?>(null) }
        var showSuccess by remember { mutableStateOf(false) }

        // Press animation state
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        // Firebase Auth user profile image
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val userProfileUrl = currentUser?.photoUrl?.toString()

        // Observe upload state
        val uploadState by uploadViewModel.uploadState.collectAsState()

        // Handle upload state changes
        LaunchedEffect(uploadState) {
            when (uploadState) {
                is StoryUploadViewModel.UploadState.Success -> {
                    isUploading = false
                    showCaptionDialog = false
                    showSuccess = true
                    delay(1500)
                    showSuccess = false
                    selectedImageUri = null
                    // Pass the created Story object back
                    onStoryCreated((uploadState as StoryUploadViewModel.UploadState.Success).story)
                    uploadViewModel.resetUploadState()
                }

                is StoryUploadViewModel.UploadState.Error -> {
                    isUploading = false
                    showError = (uploadState as StoryUploadViewModel.UploadState.Error).message
                }

                is StoryUploadViewModel.UploadState.Uploading -> {
                    isUploading = true
                }

                else -> {
                    isUploading = false
                }
            }
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                showCaptionDialog = true
            }
        }

        val interactionSource = remember { MutableInteractionSource() }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = modifier
                    .width(100.dp)
                    .scale(scale)
                    .clickable(
                        enabled = !isUploading,
                        indication = null,
                        interactionSource = interactionSource
                    ) {
                        launcher.launch("image/*")
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular story container
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated gradient border for uploading
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF833AB4),
                                            Color(0xFFE1306C),
                                            Color(0xFFFD1D1D),
                                            Color(0xFFF77737),
                                            Color(0xFF833AB4)
                                        )
                                    )
                                )
                        )
                    }

                    // Outer circle with border
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isUploading) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = when {
                                    isUploading -> Color.Transparent
                                    showSuccess -> Color(0xFF4CAF50)
                                    else -> Color(0xFF3A3A3A)
                                },
                                shape = CircleShape
                            )
                            .background(Color(0xFF262626)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner profile image circle
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfileUrl != null) {
                                AsyncImage(
                                    model = userProfileUrl,
                                    contentDescription = "Your profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E8E),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Loading indicator overlay
                            if (isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            // Success checkmark animation
                            if (showSuccess) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        color = Color.White,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Blue plus button at bottom-right
                    if (!isUploading && !showSuccess) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF121212))
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0095F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add story",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Label text with animation
                AnimatedContent(
                    targetState = when {
                        isUploading -> "Uploading..."
                        showSuccess -> "Posted!"
                        else -> "Your story"
                    },
                    transitionSpec = {
                        fadeIn() + slideInVertically() togetherWith
                                fadeOut() + slideOutVertically()
                    },
                    label = "label"
                ) { text ->
                    Text(
                        text = text,
                        color = when {
                            showSuccess -> Color(0xFF4CAF50)
                            else -> Color.White
                        },
                        fontSize = 11.sp,
                        fontWeight = if (showSuccess) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Error snackbar
            if (showError != null) {
                LaunchedEffect(showError) {
                    delay(3000)
                    showError = null
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF5252)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = showError ?: "",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showError = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modern caption dialog with visibility selector
        if (showCaptionDialog && selectedImageUri != null) {
            EnhancedStoryDialog(
                imageUri = selectedImageUri!!,
                onDismiss = {
                    showCaptionDialog = false
                    selectedImageUri = null
                },
                onConfirm = { caption, visibility, duration ->
                    scope.launch {
                        val imageFile = uriToFile(context, selectedImageUri!!)
                        if (imageFile != null) {
                            uploadViewModel.uploadStoryWithOptions(
                                userId = userId,
                                imageFile = imageFile,
                                caption = caption,
                                visibility = visibility,
                                duration = duration
                            )
                        } else {
                            showError = "Failed to process image"
                            showCaptionDialog = false
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun EnhancedStoryDialog(
        imageUri: Uri,
        onDismiss: () -> Unit,
        onConfirm: (caption: String, visibility: Story.Visibility, duration: Int) -> Unit
    ) {
        var caption by remember { mutableStateOf("") }
        var selectedVisibility by remember { mutableStateOf(Story.Visibility.PUBLIC) }
        var selectedDuration by remember { mutableStateOf(5) }
        var showVisibilityMenu by remember { mutableStateOf(false) }
        var showDurationMenu by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { if (!isUploading) onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isUploading,
                dismissOnClickOutside = !isUploading
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1C1E)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New Story",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = { if (!isUploading) onDismiss() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Image preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Caption input
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            enabled = !isUploading,
                            placeholder = {
                                Text(
                                    "Add a caption...",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.Gray,
                                focusedBorderColor = Color(0xFF0095F6),
                                unfocusedBorderColor = Color(0xFF3A3A3A),
                                disabledBorderColor = Color(0xFF3A3A3A),
                                cursorColor = Color(0xFF0095F6),
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Visibility selector
                        Text(
                            text = "Who can view this story?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isUploading) {
                                        showVisibilityMenu = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2C2C2E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = getVisibilityIcon(selectedVisibility),
                                            contentDescription = null,
                                            tint = Color(0xFF0095F6),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = getVisibilityTitle(selectedVisibility),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = getVisibilityDescription(selectedVisibility),
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showVisibilityMenu,
                                onDismissRequest = { showVisibilityMenu = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color(0xFF2C2C2E))
                            ) {
                                Story.Visibility.entries.forEach { visibility ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = getVisibilityIcon(visibility),
                                                    contentDescription = null,
                                                    tint = if (selectedVisibility == visibility)
                                                        Color(0xFF0095F6) else Color.Gray,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = getVisibilityTitle(visibility),
                                                        color = if (selectedVisibility == visibility)
                                                            Color.White else Color.LightGray,
                                                        fontWeight = if (selectedVisibility == visibility)
                                                            FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = getVisibilityDescription(visibility),
                                                        color = Color.Gray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedVisibility = visibility
                                            showVisibilityMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Duration selector
                        Text(
                            text = "Story duration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isUploading) { showDurationMenu = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2C2C2E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = Color(0xFF0095F6),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "$selectedDuration seconds per view",
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDurationMenu,
                                onDismissRequest = { showDurationMenu = false },
                                modifier = Modifier.background(Color(0xFF2C2C2E))
                            ) {
                                listOf(3, 5, 7, 10, 15).forEach { duration ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$duration seconds",
                                                color = if (selectedDuration == duration)
                                                    Color.White else Color.LightGray,
                                                fontWeight = if (selectedDuration == duration)
                                                    FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedDuration = duration
                                            showDurationMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel button
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isUploading,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = Color.Gray
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Share button
                            Button(
                                onClick = {
                                    isUploading = true
                                    onConfirm(caption, selectedVisibility, selectedDuration)
                                },
                                enabled = !isUploading,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0095F6),
                                    disabledContainerColor = Color(0xFF0095F6).copy(alpha = 0.5f)
                                )
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Uploading...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Share Story",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Upload overlay with blur effect
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable(enabled = false) { },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth(0.8f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2C2C2E)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Animated gradient progress indicator
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFF0095F6),
                                            strokeWidth = 6.dp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Color(0xFF0095F6),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "Uploading Your Story",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Please wait while we upload your story...",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Progress steps
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        UploadStep(
                                            text = "Processing image",
                                            isActive = true
                                        )
                                        UploadStep(
                                            text = "Uploading to cloud",
                                            isActive = true
                                        )
                                        UploadStep(
                                            text = "Saving to database",
                                            isActive = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun UploadStep(
        text: String,
        isActive: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF0095F6),
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                )
            }
            Text(
                text = text,
                fontSize = 14.sp,
                color = if (isActive) Color.White else Color.Gray
            )
        }
    }

    private fun getVisibilityIcon(visibility: Story.Visibility): ImageVector {
        return when (visibility) {
            Story.Visibility.PUBLIC -> Icons.Default.Public
            Story.Visibility.FRIENDS -> Icons.Default.Group
            Story.Visibility.CLOSE_FRIENDS -> Icons.Default.Star
            Story.Visibility.CUSTOM -> Icons.Default.PersonAdd
            Story.Visibility.PRIVATE -> Icons.Default.Lock
        }
    }

    private fun getVisibilityTitle(visibility: Story.Visibility): String {
        return when (visibility) {
            Story.Visibility.PUBLIC -> "Public"
            Story.Visibility.FRIENDS -> "Friends"
            Story.Visibility.CLOSE_FRIENDS -> "Close Friends"
            Story.Visibility.CUSTOM -> "Custom"
            Story.Visibility.PRIVATE -> "Only Me"
        }
    }

    private fun getVisibilityDescription(visibility: Story.Visibility): String {
        return when (visibility) {
            Story.Visibility.PUBLIC -> "Anyone can view"
            Story.Visibility.FRIENDS -> "Only your friends"
            Story.Visibility.CLOSE_FRIENDS -> "Select close friends"
            Story.Visibility.CUSTOM -> "Selected people"
            Story.Visibility.PRIVATE -> "Only visible to you"
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.jpg")
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
}