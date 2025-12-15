package com.nidoham.socialsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidoham.socialsphere.people.repository.PeopleSuggestionsRepositoryImpl
import com.nidoham.socialsphere.ui.screens.*
import com.nidoham.socialsphere.ui.theme.SocialSphereTheme
import com.nidoham.socialsphere.ui.viewmodel.PeopleSuggestionsViewModel
import com.nidoham.socialsphere.ui.viewmodel.PeopleSuggestionsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocialSphereTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchDialog by remember { mutableStateOf(false) }

    // Firebase & ViewModel Initialization
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    // বর্তমান ইউজার আইডি (Null Safety সহ)
    val currentUid = auth.currentUser?.uid ?: ""

    // Repository এবং ViewModel Factory সেটআপ
    val repository = remember { PeopleSuggestionsRepositoryImpl(firestore) }
    val factory = remember { PeopleSuggestionsViewModelFactory(repository) }

    // ViewModel তৈরি করা
    val friendsViewModel: PeopleSuggestionsViewModel = viewModel(factory = factory)

    Scaffold(
        topBar = {
            AppTopBar(
                onSearchClick = { showSearchDialog = true },
                onNotificationClick = { /* Handle notification */ },
                onProfileClick = { /* Handle profile */ }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> ChatsScreen()
                2 -> FriendsScreen(
                    viewModel = friendsViewModel,
                    currentUid = currentUid
                )
                3 -> StreamScreen()
                4 -> MarketsScreen()
            }
        }

        // Search Dialog
        if (showSearchDialog) {
            SimpleSearchDialog(
                onDismiss = { showSearchDialog = false }
            )
        }
    }
}

@Composable
fun SimpleSearchDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Search functionality coming soon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SocialSphere",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Text("3")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AppBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", Icons.Default.Home),
        NavigationItem("Chats", Icons.Default.Chat),
        NavigationItem("Friends", Icons.Default.People),
        NavigationItem("Streams ", Icons.Default.VideoLibrary),
        NavigationItem("Markets", Icons.Default.Store)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp
                    )
                },
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector
)