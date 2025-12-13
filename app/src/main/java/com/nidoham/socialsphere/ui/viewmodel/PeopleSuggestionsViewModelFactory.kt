package com.nidoham.socialsphere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nidoham.socialsphere.people.repository.PeopleSuggestionsRepository

class PeopleSuggestionsViewModelFactory(
    private val repository: PeopleSuggestionsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeopleSuggestionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PeopleSuggestionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}