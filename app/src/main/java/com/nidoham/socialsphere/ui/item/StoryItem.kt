// StoryItem.kt - Fixed version with StoryVisibility enum and null-safe avatar handling

package com.nidoham.socialsphere.ui.item

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nidoham.social.stories.StoryWithAuthor
import com.nidoham.socialsphere.ui.theme.*

// <CHANGE> Added missing StoryVisibility enum
enum class StoryVisibility {
    PUBLIC,
    FRIENDS,
    CUSTOM,
    PRIVATE
}

/* ----------------------------- STORY ITEM ----------------------------- */

@Composable
fun StoryItem(
    story: StoryWithAuthor,
    modifier: Modifier = Modifier,
    onClick: (StoryWithAuthor) -> Unit
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .clickable { onClick(story) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {

            // Gradient Ring
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (story.isActive()) {
                            Brush.linearGradient(
                                listOf(
                                    StoryGradientStart,
                                    StoryGradientMiddle,
                                    StoryGradientEnd
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(BorderColor, BorderColor))
                        }
                    )
            )

            // Inner gap
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkBackground)
            )

            // <CHANGE> Handle nullable authorAvatar with fallback placeholder
            if (!story.authorAvatar.isNullOrEmpty()) {
                AsyncImage(
                    model = story.authorAvatar,
                    contentDescription = "Story Avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback when avatar is null or empty
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DarkCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = story.authorUsername,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
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
            .width(88.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BorderColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your Story",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontSize = 11.sp,
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