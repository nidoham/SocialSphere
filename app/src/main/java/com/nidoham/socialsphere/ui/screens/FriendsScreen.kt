package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.ui.components.FriendItem
import com.nidoham.socialsphere.ui.components.FriendRequestItem
import com.nidoham.socialsphere.ui.components.SuggestedFriendItem

@Composable
fun FriendsScreen(
    //viewModel: PeopleSuggestionsViewModel,
    currentUid: String
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
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
           // 0 -> PeopleYouMayKnowContent(viewModel, currentUid)
            1 -> FriendRequestsContent()
            2 -> AllFriendsContent()
        }
    }
}

@Composable
private fun PeopleYouMayKnowContent(
    //viewModel: PeopleSuggestionsViewModel,
    currentUid: String
) {
    // ViewModel থেকে স্টেট অবজারভ করা
    //val suggestions by viewModel.suggestions.collectAsState()
    //val isLoading by viewModel.isLoading.collectAsState()

    // স্ক্রিন লোড হলে বা প্রথমবার ডেটা ফেচ করার জন্য
    LaunchedEffect(currentUid) {
        //if (suggestions.isEmpty() && currentUid.isNotEmpty()) {
         //   viewModel.loadSuggestions(currentUid)
        //}
    }

    Box(modifier = Modifier.fillMaxSize()) {
       // if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      //  } else {
            //if (suggestions.isEmpty()) {
                // যদি কোনো সাজেশন না থাকে
                Text(
                    text = "No suggestions available right now.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
         //   } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // itemsIndexed ব্যবহার করে index কে key হিসেবে ব্যবহার করা হলো
                    // এতে duplicate key এর সমস্যা হবে না
                   /* itemsIndexed(
                        items = suggestions,
                        key = { index, _ -> "suggestion_$index" }
                    ) { index, (uid, accountData) ->
                        // Account ক্লাসের nested structure অনুযায়ী name অ্যাক্সেস করা
                        //val userName = accountData.account.name.ifEmpty { "Unknown User" }

                       /* SuggestedFriendItem(
                           // userName = userName,
                            mutualInfo = "New to SocialSphere",
                            //profilePictureUrl = accountData.account.profilePictureUrl,
                            onAddFriend = {
                                //viewModel.sendFriendRequest(uid)
                            },
                            onRemove = {
                                //viewModel.removeSuggestion(uid)
                            }
                        ) */
                    } */
                }
            }
       // }
    //}
}

@Composable
private fun FriendRequestsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(3) { index ->
            FriendRequestItem(
                userName = "Requester User ${index + 1}",
                mutualFriends = "${(2..10).random()} mutual friends",
                onAccept = { /* Handle accept logic */ },
                onDecline = { /* Handle decline logic */ }
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
                userName = "My Friend $index",
                status = "Active ${(1..24).random()} hours ago",
                onClick = { /* Navigate to profile */ },
                onMessageClick = { /* Open chat */ }
            )
        }
    }
}