package com.nidoham.socialsphere.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Category list constant
object CategoryConstants {
    val CATEGORIES = listOf(
        "All",
        "Gaming",
        "Sports",
        "Music",
        "Movies",
        "News",
        "Live",
        "Technology",
        "Comedy",
        "Education",
        "Fashion",
        "Cooking"
    )
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
        modifier = modifier.height(36.dp)
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

@Composable
fun CategoryChipRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<String> = CategoryConstants.CATEGORIES
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { category -> category }
        ) { category ->
            CategoryChip(
                text = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}