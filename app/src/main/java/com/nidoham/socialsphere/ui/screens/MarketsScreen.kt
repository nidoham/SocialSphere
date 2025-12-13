package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nidoham.socialsphere.ui.components.MarketItem

@Composable
fun MarketsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            Text(
                text = "Marketplace",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        items(10) { index ->
            MarketItem(
                productName = "Product Name $index",
                category = "Electronics",
                price = "$${(index + 1) * 50}",
                onViewDetails = { /* Handle view details */ }
            )
        }
    }
}