package com.nidoham.socialsphere.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class Category(val name: String)
data class Video(
    val id: String,
    val thumbnail: String,
    val title: String,
    val channel: String,
    val views: String,
    val timestamp: String,
    val duration: String,
    val channelAvatar: String
)

@Composable
fun StreamScreen() {
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        Category("All"),
        Category("Gaming"),
        Category("Sports"),
        Category("Songs"),
        Category("Movies"),
        Category("News"),
        Category("Live"),
        Category("Technology"),
        Category("Comedy")
    )

    val videos = remember {
        List(20) { index ->
            Video(
                id = "$index",
                thumbnail = "https://picsum.photos/seed/$index/480/270",
                title = when (index % 5) {
                    0 -> "Amazing Gaming Moments That Will Blow Your Mind"
                    1 -> "Top 10 Sports Highlights of the Week"
                    2 -> "Best Music Mix 2024 | Chill Vibes Playlist"
                    3 -> "Movie Review: Latest Blockbuster Analysis"
                    else -> "Trending Video Content You Don't Want to Miss"
                },
                channel = listOf("TechGuru", "GameMaster", "SportsFan", "MusicVibes", "MovieBuff")[index % 5],
                views = listOf("1.2M", "856K", "2.5M", "342K", "1.8M")[index % 5],
                timestamp = listOf("2 days ago", "5 hours ago", "1 week ago", "3 days ago", "12 hours ago")[index % 5],
                duration = listOf("12:34", "8:45", "15:20", "6:12", "20:15")[index % 5],
                channelAvatar = "https://picsum.photos/seed/avatar$index/100/100"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category Chips
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category.name,
                        onClick = { selectedCategory = category.name },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Video List
        items(videos) { video ->
            VideoCard(video = video)
        }
    }
}

@Composable
fun VideoCard(video: Video) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Duration Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Video Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Channel Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = video.channelAvatar,
                    contentDescription = video.channel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Title and Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${video.channel} • ${video.views} views • ${video.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // More Options
            IconButton(
                onClick = { /* Handle options */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}