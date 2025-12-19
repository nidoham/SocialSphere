package com.nidoham.socialsphere

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.ui.screen.*
import com.nidoham.socialsphere.ui.theme.DarkBackground
import com.nidoham.socialsphere.ui.theme.IconActive
import com.nidoham.socialsphere.ui.theme.IconInactive
import com.nidoham.socialsphere.ui.theme.NotificationDot
import com.nidoham.socialsphere.ui.theme.Primary
import com.nidoham.socialsphere.ui.theme.PrimaryContainer
import com.nidoham.socialsphere.ui.theme.SocialSphereTheme
import com.nidoham.socialsphere.ui.theme.TextPrimary
import com.nidoham.socialsphere.ui.theme.TextTertiary

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
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = { AppTopBar() },
        bottomBar = {
            AppBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    if (index == 2) {
                        // Open CreateActivity when middle button is clicked
                        val intent = Intent(context, CreatePostActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        selectedTab = index
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> ChatScreen()
                3 -> FriendsScreen()
                4 -> MarketsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary
                )
            }
        },
        actions = {
            // Search Icon
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = IconActive,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notification Icon with Badge
            Box(
                modifier = Modifier.padding(end = 4.dp)
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = IconActive,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Notification Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 8.dp)
                        .size(18.dp)
                        .background(NotificationDot, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = TextPrimary,
            actionIconContentColor = IconActive
        )
    )
}

@Composable
fun AppBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavigationItem("Chats", Icons.Outlined.Chat, Icons.Filled.Chat),
        NavigationItem("Create", Icons.Outlined.Create, Icons.Filled.Create),
        NavigationItem("Friends", Icons.Outlined.People, Icons.Filled.People),
        NavigationItem("Markets", Icons.Outlined.Store, Icons.Filled.Store)
    )

    Surface(
        color = DarkBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = DarkBackground,
            contentColor = IconActive,
            tonalElevation = 0.dp,
            modifier = Modifier.height(70.dp)
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == index) item.iconFilled else item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = PrimaryContainer.copy(alpha = 0.2f),
                        unselectedIconColor = IconInactive,
                        unselectedTextColor = TextTertiary
                    )
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val iconFilled: ImageVector
)