package com.nidoham.socialsphere.ui.image

import org.schabi.newpipe.extractor.Image

class ImageBitmap {
    companion object {
        fun url(thumbnails: List<Image>): String {
            return thumbnails.maxByOrNull { it.width * it.height }?.url ?: thumbnails.get(0).url
        }
    }
}