package com.nidoham.socialsphere.search

data class SearchResult(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: String,
    val imageUrl: String? = null,
    val extraData: Map<String, Any>? = null
)