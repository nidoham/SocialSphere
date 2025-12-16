package com.nidoham.socialsphere.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.socialsphere.database.cloud.model.Story
import com.nidoham.socialsphere.database.cloud.model.User
import com.nidoham.socialsphere.database.cloud.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "StoryItem"

@Composable
fun StoryItem(
    isAddStory: Boolean = false,
    story: Story? = null,
    onClick: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch user data
    LaunchedEffect(isAddStory, story?.userId) {
        isLoading = true
        try {
            if (isAddStory) {
                // For "Add Story", fetch current user
                val uid = currentUser?.uid
                Log.d(TAG, "=== ADD STORY MODE ===")
                Log.d(TAG, "Current user UID: $uid")

                user = uid?.let {
                    fetchUserById(it)
                } ?: run {
                    Log.e(TAG, "❌ No authenticated user found")
                    null
                }
            } else {
                // For regular story, fetch story owner
                val userId = story?.userId
                Log.d(TAG, "=== REGULAR STORY MODE ===")
                Log.d(TAG, "Story ID: ${story?.id}")
                Log.d(TAG, "Story User ID: $userId")
                Log.d(TAG, "Story Image URL: ${story?.imageUrl}")

                if (userId.isNullOrBlank()) {
                    Log.e(TAG, "❌ Story userId is null or blank!")
                    user = null
                } else {
                    user = fetchUserById(userId)
                }
            }

            // Log final user state
            Log.d(TAG, "Final user state:")
            Log.d(TAG, "  - User ID: ${user?.id}")
            Log.d(TAG, "  - Display Name: ${user?.displayName}")
            Log.d(TAG, "  - Username: ${user?.username}")
            Log.d(TAG, "  - Avatar URL: ${user?.avatarUrl}")
            Log.d(TAG, "  - Email: ${user?.email}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in LaunchedEffect: ${e.message}", e)
        } finally {
            isLoading = false
            Log.d(TAG, "Loading complete. isLoading = false")
        }
    }

    if (isAddStory) {
        AddStoryItem(
            user = user,
            currentUser = currentUser,
            isLoading = isLoading,
            onClick = onClick
        )
    } else {
        RegularStoryItem(
            story = story,
            storyUser = user,
            isLoading = isLoading,
            onClick = onClick
        )
    }
}

@Composable
private fun AddStoryItem(
    user: User?,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section - Profile Picture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    // Try Firestore user first
                    !user?.avatarUrl.isNullOrBlank() -> {
                        Image(
                            painter = rememberAsyncImagePainter(user?.avatarUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Fallback to Firebase Auth photo
                    currentUser?.photoUrl != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(currentUser.photoUrl.toString()),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Default icon
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.6f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Bottom section - "Create" text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Story",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Add button overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 95.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(3.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Story",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RegularStoryItem(
    story: Story?,
    storyUser: User?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Log.d(TAG, "🎨 RegularStoryItem Composing")
    Log.d(TAG, "   Story: ${story?.id}")
    Log.d(TAG, "   User: ${storyUser?.displayName}")
    Log.d(TAG, "   Avatar: ${storyUser?.avatarUrl}")
    Log.d(TAG, "   Loading: $isLoading")

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(Color(0xFF1C1C1E))
    ) {
        // Story image background
        story?.imageUrl?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(
                    model = url,
                    onError = { error ->
                        Log.e(TAG, "❌ Story image load failed: $url")
                        Log.e(TAG, "   Error: ${error.result.throwable.message}")
                    },
                    onSuccess = {
                        Log.d(TAG, "✅ Story image loaded: $url")
                    }
                ),
                contentDescription = "Story image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // Profile picture with border
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(42.dp)
                .border(3.dp, Color(0xFF1877F2), CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Log.d(TAG, "📊 Showing loading indicator")
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1877F2)
                    )
                }
                !storyUser?.avatarUrl.isNullOrBlank() -> {
                    Log.d(TAG, "🖼️ Attempting to load avatar: ${storyUser?.avatarUrl}")

                    val painter = rememberAsyncImagePainter(
                        model = storyUser?.avatarUrl,
                        contentScale = ContentScale.Crop,
                        onError = { error ->
                            Log.e(TAG, "❌ Avatar load FAILED!")
                            Log.e(TAG, "   URL: ${storyUser?.avatarUrl}")
                            Log.e(TAG, "   Error: ${error.result.throwable.message}")
                            error.result.throwable.printStackTrace()
                        },
                        onSuccess = {
                            Log.d(TAG, "✅ Avatar loaded successfully!")
                            Log.d(TAG, "   URL: ${storyUser?.avatarUrl}")
                        },
                        onLoading = {
                            Log.d(TAG, "⏳ Avatar loading...")
                        }
                    )

                    // Log painter state
                    val painterState = painter.state
                    Log.d(TAG, "Painter state: ${painterState::class.simpleName}")

                    Image(
                        painter = painter,
                        contentDescription = "User avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Log.d(TAG, "👤 Using default Person icon")
                    Log.d(TAG, "   Reason: avatarUrl = '${storyUser?.avatarUrl}'")
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default avatar",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Username at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp, 8.dp)
        ) {
            val displayText = when {
                isLoading -> "Loading..."
                storyUser?.displayName?.isNotBlank() == true -> storyUser.displayName!!
                storyUser?.username?.isNotBlank() == true -> storyUser.username
                else -> "Unknown"
            }

            Log.d(TAG, "📝 Display text: $displayText")

            Text(
                text = displayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                lineHeight = 14.sp
            )
        }
    }
}

private suspend fun fetchUserById(userId: String): User? {
    return withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔍 FETCHING USER FROM FIRESTORE")
            Log.d(TAG, "   Collection: /users")
            Log.d(TAG, "   Document ID: $userId")

            val repository = UserRepository.getInstance()
            val user = repository.getUserById(userId)

            if (user != null) {
                Log.d(TAG, "✅ USER FOUND IN FIRESTORE!")
                Log.d(TAG, "   ID: ${user.id}")
                Log.d(TAG, "   Username: ${user.username}")
                Log.d(TAG, "   Display Name: ${user.displayName}")
                Log.d(TAG, "   Email: ${user.email}")
                Log.d(TAG, "   Avatar URL: ${user.avatarUrl}")
                Log.d(TAG, "   Avatar is blank: ${user.avatarUrl.isNullOrBlank()}")
                Log.d(TAG, "   Created At: ${user.createdAt}")
            } else {
                Log.e(TAG, "❌ USER NOT FOUND!")
                Log.e(TAG, "   The document /users/$userId does not exist")
                Log.e(TAG, "   Please check:")
                Log.e(TAG, "   1. Is the user created in Firestore?")
                Log.e(TAG, "   2. Is the userId correct?")
                Log.e(TAG, "   3. Are Firestore rules allowing read access?")
            }
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            user
        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ EXCEPTION WHILE FETCHING USER")
            Log.e(TAG, "   User ID: $userId")
            Log.e(TAG, "   Exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Message: ${e.message}")
            e.printStackTrace()
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            null
        }
    }
}