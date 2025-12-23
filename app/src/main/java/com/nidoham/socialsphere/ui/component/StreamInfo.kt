package com.nidoham.socialsphere.ui.component

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream

data class StreamInfo(
    val id: String,
    val name: String,
    val uploader: String,
    val thumbnails: List<Image>,
    val url: String,
    val audioStreams: List<AudioStream>,
    val videoStreams: List<VideoStream>,
    val duration: Long,
    val viewCount: Long
) {
    companion object {
        fun from(item: org.schabi.newpipe.extractor.stream.StreamInfo): StreamInfo {
            return StreamInfo(
                id = item.id,
                name = item.name ?: "Unknown",
                uploader = item.uploaderName ?: "Unknown",
                thumbnails = item.thumbnails,
                url = item.url,
                audioStreams = item.audioStreams ?: emptyList(),
                videoStreams = item.videoStreams ?: emptyList(),
                duration = item.duration,
                viewCount = item.viewCount
            )
        }
    }

    fun thumbnailUrl(): String {
        return thumbnails.maxByOrNull { it.width * it.height }?.url ?: thumbnails.get(0).url
    }
}