package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReelsScreen() {
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        "All",
        "Gaming",
        "Sports",
        "News",
        "Music",
        "Comedy",
        "Education",
        "Technology",
        "Food",
        "Travel"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top category tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(categories) { category ->
                CategoryChip(
                    text = category,
                    isSelected = category == selectedCategory,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Reels",
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reels Feature",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Coming Soon!",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Selected: $selectedCategory",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}