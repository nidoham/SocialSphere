package com.nidoham.socialsphere.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.nidoham.social.model.Story
import com.nidoham.socialsphere.ui.theme.*

/**
 * Instagram-style Story item component with circular shape.
 */
@Composable
fun StoryItem(
    story: Story,
    isFirstItem: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Story) -> Unit = {}
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
            // Gradient ring border (only for active stories, not first item)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = when {
                            isFirstItem -> Brush.linearGradient(
                                colors = listOf(BorderColor, BorderColor)
                            )
                            story.isActive() -> Brush.linearGradient(
                                colors = listOf(
                                    StoryGradientStart,
                                    StoryGradientMiddle,
                                    StoryGradientEnd
                                )
                            )
                            else -> Brush.linearGradient(
                                colors = listOf(BorderColor, BorderColor)
                            )
                        }
                    )
            )

            // Dark background gap (creates the ring effect)
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(DarkBackground)
            )

            AsyncImage(
                model = story.getPrimaryMediaUrl(),
                contentDescription = if (isFirstItem) "Add Story" else "View Story",
                modifier = Modifier
                    .size(95.dp)
                    .clip(CircleShape)
                    .background(DarkCard),
                contentScale = ContentScale.Crop
            )

            if (isFirstItem) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Username label
        Text(
            text = if (isFirstItem) "Your Story" else story.authorId,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp
        )
    }
}

/**
 * A horizontal row helper to display stories.
 */
@Composable
fun StoryItemRow(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Story Button
        StoryItem(
            story = Story(
                id = "add_story",
                authorId = "current_user",
                caption = ""
            ),
            isFirstItem = true,
            onClick = { onAddStoryClick() }
        )

        // List of active stories
        stories.filter { it.isActive() }.forEach { story ->
            StoryItem(
                story = story,
                isFirstItem = false,
                onClick = onStoryClick
            )
        }
    }
}
