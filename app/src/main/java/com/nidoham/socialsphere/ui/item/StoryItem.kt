package com.nidoham.socialsphere.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nidoham.social.stories.StoryWithAuthor
import com.nidoham.socialsphere.ui.theme.*

/* ----------------------------- STORY ITEM ----------------------------- */

@Composable
fun StoryItem(
    story: StoryWithAuthor,
    modifier: Modifier = Modifier,
    onClick: (StoryWithAuthor) -> Unit
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clickable { onClick(story) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated gradient border for active stories
            if (story.isActive()) {
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
                    .padding(if (story.isActive()) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (story.isActive()) Color.Transparent else Color(0xFF3A3A3A),
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
                    // Show story image or author avatar
                    val imageUrl = if (story.mediaUrls.isNotEmpty()) {
                        story.mediaUrls.first()
                    } else {
                        story.authorAvatar
                    }

                    if (!imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Story preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback when no image available
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF2C2C2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = Color(0xFF8E8E8E),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = story.authorUsername,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )
    }
}

/* ----------------------------- ADD STORY ----------------------------- */

@Composable
fun AddStoryItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer circle with border
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
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF8E8E8E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Blue plus button at bottom-right
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

/* ----------------------------- ROW ----------------------------- */

@Composable
fun StoryItemRow(
    stories: List<StoryWithAuthor>,
    modifier: Modifier = Modifier,
    onStoryClick: (StoryWithAuthor) -> Unit,
    onAddStoryClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AddStoryItem(onClick = onAddStoryClick)

        stories
            .filter { it.isActive() }
            .distinctBy { it.authorId }
            .forEach { story ->
                StoryItem(
                    story = story,
                    onClick = onStoryClick
                )
            }
    }
}