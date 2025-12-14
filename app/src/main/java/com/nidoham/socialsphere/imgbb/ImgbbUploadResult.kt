package com.nidoham.socialsphere.imgbb

data class ImgbbUploadResult(
    val success: Boolean,
    val imageUrl: String?,
    val errorMessage: String?
)