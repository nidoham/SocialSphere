package com.nidoham.socialsphere.search.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EventsCategoryView(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Event,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(16.dp))
            Text("Search Events", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (query.isNotEmpty()) {
                Text("Query: \"$query\"", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}