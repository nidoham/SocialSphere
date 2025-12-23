package com.nidoham.socialsphere.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nidoham.socialsphere.ui.component.StreamInfoItem

@Composable
fun VideoCard(stream: StreamInfoItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle video click */ }
            .padding(bottom = 16.dp)
    ) {
        // Video thumbnail
        AsyncImage(
            model = stream.thumbnailUrl(),
            contentDescription = stream.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )

        // Video info row
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Channel avatar
            AsyncImage(
                model = stream.thumbnails.firstOrNull()?.url,
                contentDescription = stream.uploader,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Video details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.name,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stream.uploader,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}