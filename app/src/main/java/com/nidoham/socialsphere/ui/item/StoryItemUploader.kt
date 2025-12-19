package com.nidoham.socialsphere.ui.item

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.social.user.UserExtractor
import com.nidoham.socialsphere.CreateStoriesActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Story uploader item
 * Fetches current user using Firebase UID and shows avatar
 */
object StoryItemUploader {

    @Composable
    fun StoryUploadButton(
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val scope = rememberCoroutineScope()

        // Press animation
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        // Avatar state
        var avatarUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var hasError by remember { mutableStateOf(false) }

        // Fetch user avatar using UID
        LaunchedEffect(uid) {
            if (uid != null) {
                scope.launch {
                    try {
                        isLoading = true
                        hasError = false

                        // Use application context to avoid memory leaks
                        val appContext = context.applicationContext
                        val extractor = UserExtractor(appContext)

                        // Fetch user on IO dispatcher
                        val result = withContext(Dispatchers.IO) {
                            extractor.fetchCurrentUser(uid)
                        }

                        if (result.isSuccess) {
                            val user = result.getOrNull()
                            avatarUrl = user?.avatarUrl

                            // Fallback to Firebase profile photo if avatarUrl is empty
                            if (avatarUrl.isNullOrEmpty()) {
                                avatarUrl = currentUser.photoUrl?.toString()
                            }
                        } else {
                            // Fallback to Firebase profile photo on error
                            avatarUrl = currentUser.photoUrl?.toString()
                            hasError = true
                        }
                    } catch (e: Exception) {
                        // Fallback to Firebase profile photo
                        avatarUrl = currentUser?.photoUrl?.toString()
                        hasError = true
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                isLoading = false
                hasError = true
            }
        }

        Column(
            modifier = modifier
                .width(100.dp)
                .scale(scale)
                .clickable(
                    onClick = { navigateToCreateStory(context) },
                    onClickLabel = "Create Story"
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF3A3A3A),
                            shape = CircleShape
                        )
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner avatar circle
                    Box(
                        modifier = Modifier
                            .size(95.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // Show loading indicator
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF0095F6),
                                    strokeWidth = 2.dp
                                )
                            }
                            // Show avatar if available
                            !avatarUrl.isNullOrEmpty() -> {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Your profile picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        // If image fails to load, show fallback icon
                                        hasError = true
                                    }
                                )
                            }
                            // Show fallback icon
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "No profile image",
                                    tint = Color(0xFF8E8E8E),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Plus button overlay (bottom-right)
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your story",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    /**
     * Navigate to CreateStoriesActivity
     */
    private fun navigateToCreateStory(context: Context) {
        val intent = Intent(context, CreateStoriesActivity::class.java)
        context.startActivity(intent)
    }
}