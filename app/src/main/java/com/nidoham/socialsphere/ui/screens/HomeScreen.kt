package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.ui.components.PostCard
import com.nidoham.socialsphere.ui.components.StorySection

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Story Section
        item {
            StorySection()
        }

        // Posts
        items(10) { index ->
            PostCard(
                userName = "User Name $index",
                timeAgo = "2 hours ago",
                content = "This is a sample post content. It can contain text, images, or videos. #SocialSphere #ConnectWithWorld",
                likeCount = "125",
                commentCount = "45",
                shareCount = "12",
                onLikeClick = { /* Handle like */ },
                onCommentClick = { /* Handle comment */ },
                onShareClick = { /* Handle share */ },
                onMoreClick = { /* Handle more options */ }
            )
        }
    }
}