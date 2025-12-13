package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.ui.components.FriendItem
import com.nidoham.socialsphere.ui.components.FriendRequestItem
import com.nidoham.socialsphere.ui.components.SuggestedFriendItem

@Composable
fun FriendsScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Suggestions", "Requests", "Friends")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Layout
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> PeopleYouMayKnowContent()
            1 -> FriendRequestsContent()
            2 -> AllFriendsContent()
        }
    }
}

@Composable
private fun PeopleYouMayKnowContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(15) { index ->
            SuggestedFriendItem(
                userName = "Suggested User ${index + 1}",
                mutualInfo = "${(5..25).random()} mutual friends",
                onAddFriend = { /* Handle add friend */ },
                onRemove = { /* Handle remove suggestion */ }
            )
        }
    }
}

@Composable
private fun FriendRequestsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(3) { index ->
            FriendRequestItem(
                userName = "User ${index + 1}",
                mutualFriends = "${(2..10).random()} mutual friends",
                onAccept = { /* Handle accept */ },
                onDecline = { /* Handle decline */ }
            )
        }
    }
}

@Composable
private fun AllFriendsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(20) { index ->
            FriendItem(
                userName = "Friend Name $index",
                status = "Active ${(1..24).random()} hours ago",
                onClick = { /* Handle friend click */ },
                onMessageClick = { /* Handle message click */ }
            )
        }
    }
}