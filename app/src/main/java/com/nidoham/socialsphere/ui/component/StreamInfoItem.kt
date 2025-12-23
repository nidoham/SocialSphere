package com.nidoham.socialsphere.ui.component

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.StreamInfoItem as NewPipeStreamInfoItem

data class StreamInfoItem(
    val name: String,
    val uploader: String,
    val thumbnails: List<Image>,
    val duration: Long
) {
    companion object {
        fun from(item: NewPipeStreamInfoItem): StreamInfoItem {
            return StreamInfoItem(
                name = item.name ?: "Unknown",
                uploader = item.uploaderName ?: "Unknown",
                thumbnails = item.thumbnails ?: emptyList(),
                duration = item.duration
            )
        }
    }

    fun thumbnailUrl(): String {
        return thumbnails.maxByOrNull { it.width * it.height }?.url ?: thumbnails.get(0).url
    }
}