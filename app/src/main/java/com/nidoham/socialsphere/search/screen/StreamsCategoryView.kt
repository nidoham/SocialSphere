package com.nidoham.socialsphere.search.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreamsCategoryView(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(
                    Icons.Default.Sensors,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Red
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red
                ) {
                    Text(
                        "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Search Live Streams", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (query.isNotEmpty()) {
                Text("Query: \"$query\"", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}