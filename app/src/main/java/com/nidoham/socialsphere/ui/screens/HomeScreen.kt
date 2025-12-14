package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nidoham.socialsphere.ui.components.PostCard
import com.nidoham.socialsphere.ui.components.StorySection
import com.nidoham.socialsphere.ui.viewmodel.StoriesViewModel

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Story Section
        item {
            val storiesViewModel: StoriesViewModel = viewModel()
            StorySection(viewModel = storiesViewModel)
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