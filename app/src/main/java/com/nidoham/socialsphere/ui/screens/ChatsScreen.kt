package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.ui.components.ChatItem

@Composable
fun ChatsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(15) { index ->
            ChatItem(
                userName = "Contact Name $index",
                lastMessage = "Last message preview...",
                time = "10:30 AM",
                isOnline = index < 3,
                unreadCount = if (index < 5) index + 1 else 0,
                onClick = { /* Handle chat click */ }
            )
        }
    }
}