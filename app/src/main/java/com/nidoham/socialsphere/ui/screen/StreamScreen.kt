package com.nidoham.socialsphere.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.socialsphere.ui.component.StreamInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem as NewPipeStreamInfoItem
import com.nidoham.socialsphere.ui.item.*

// -------------------- UI State --------------------

sealed class StreamUiState {
    object Initial : StreamUiState()
    object Loading : StreamUiState()
    data class Success(
        val streams: List<StreamInfoItem>,
        val isAppending: Boolean = false
    ) : StreamUiState()
    data class Error(val message: String) : StreamUiState()
}

// -------------------- ViewModel --------------------

class StreamViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Initial)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private var nextPage: Page? = null
    private var currentJob: Job? = null
    private var currentStreams: List<StreamInfoItem> = emptyList()

    init {
        loadTrending()
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        loadContentForCategory(category)
    }

    private fun loadContentForCategory(category: String) {
        // Cancel previous job to avoid duplicate requests
        currentJob?.cancel()

        currentJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = StreamUiState.Loading
            currentStreams = emptyList()
            nextPage = null

            try {
                val youtube = ServiceList.YouTube

                when (category) {
                    "All" -> {
                        // Load trending
                        val kioskList = youtube.kioskList
                        val defaultKioskId = kioskList.defaultKioskId
                        val extractor = kioskList.getExtractorById(defaultKioskId, null)
                        extractor.fetchPage()

                        val initialPage = extractor.initialPage
                        nextPage = initialPage.nextPage

                        val newItems = initialPage.items
                            ?.mapNotNull { item ->
                                (item as? NewPipeStreamInfoItem)?.let {
                                    StreamInfoItem.from(it)
                                }
                            } ?: emptyList()

                        currentStreams = newItems
                        _uiState.value = StreamUiState.Success(newItems)
                    }
                    else -> {
                        // Search by category
                        val searchExtractor = youtube.getSearchExtractor(category)
                        searchExtractor.fetchPage()

                        val initialPage = searchExtractor.initialPage
                        nextPage = initialPage.nextPage

                        val newItems = initialPage.items
                            ?.mapNotNull { item ->
                                (item as? NewPipeStreamInfoItem)?.let {
                                    StreamInfoItem.from(it)
                                }
                            } ?: emptyList()

                        currentStreams = newItems
                        _uiState.value = StreamUiState.Success(newItems)
                    }
                }

            } catch (e: Exception) {
                _uiState.value = StreamUiState.Error("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun loadTrending() {
        loadContentForCategory("All")
    }

    fun loadMore() {
        val page = nextPage
        val currentState = _uiState.value

        if (currentState !is StreamUiState.Success || currentState.isAppending || page == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = StreamUiState.Success(currentStreams, isAppending = true)

            try {
                val youtube = ServiceList.YouTube
                val category = _selectedCategory.value

                val result = if (category == "All") {
                    val kioskList = youtube.kioskList
                    val defaultKioskId = kioskList.defaultKioskId
                    val extractor = kioskList.getExtractorById(defaultKioskId, null)
                    extractor.getPage(page)
                } else {
                    val searchExtractor = youtube.getSearchExtractor(category)
                    searchExtractor.getPage(page)
                }

                nextPage = result?.nextPage

                val newItems = result?.items
                    ?.mapNotNull { item ->
                        (item as? NewPipeStreamInfoItem)?.let {
                            StreamInfoItem.from(it)
                        }
                    } ?: emptyList()

                currentStreams = currentStreams + newItems
                _uiState.value = StreamUiState.Success(currentStreams, isAppending = false)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = StreamUiState.Success(currentStreams, isAppending = false)
            }
        }
    }
}

// -------------------- UI Components --------------------

@Composable
fun StreamScreen(
    viewModel: StreamViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Category chips at the top
        CategoryChipRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) },
            modifier = Modifier.background(Color.Black)
        )

        // Content area
        when (val state = uiState) {
            is StreamUiState.Initial -> {
                // Show nothing or placeholder
            }

            is StreamUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            is StreamUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadTrending() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is StreamUiState.Success -> {
                StreamList(
                    streams = state.streams,
                    isAppending = state.isAppending,
                    onLoadMore = { viewModel.loadMore() }
                )
            }
        }
    }
}

@Composable
private fun StreamList(
    streams: List<StreamInfoItem>,
    isAppending: Boolean,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            count = streams.size,
            key = { index -> "${streams[index].name}_$index" }
        ) { index ->
            val stream = streams[index]

            // Trigger load more when reaching the last item
            if (index == streams.lastIndex && !isAppending) {
                LaunchedEffect(index) {
                    onLoadMore()
                }
            }

            VideoCard(stream)
        }

        // Show loading indicator at bottom when appending
        if (isAppending) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}