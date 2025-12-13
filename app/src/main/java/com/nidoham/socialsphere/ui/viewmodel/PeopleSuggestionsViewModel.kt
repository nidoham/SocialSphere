package com.nidoham.socialsphere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.socialsphere.auth.model.Account
import com.nidoham.socialsphere.people.repository.PeopleSuggestionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PeopleSuggestionsViewModel(
    private val repository: PeopleSuggestionsRepository
) : ViewModel() {

    // UI-তে দেখানোর জন্য সাজেশনের লিস্ট (UID এবং Account এর পেয়ার)
    private val _suggestions = MutableStateFlow<List<Pair<String, Account>>>(emptyList())
    val suggestions: StateFlow<List<Pair<String, Account>>> = _suggestions.asStateFlow()

    // লোডিং অবস্থা বোঝার জন্য
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // এরর ম্যাসেজ স্টোর করার জন্য
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ডেটা লোড করার ফাংশন
    fun loadSuggestions(currentUid: String) {
        // যদি ইতিমধ্যে লোডিং চলছে তাহলে নতুন রিকোয়েস্ট করবে না
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Repository থেকে ৫০ জন র‍্যান্ডম ইউজার ফেচ করা
                val result = repository.getRandomPeople(currentUid, limit = 50)

                // Duplicate UIDs ফিল্টার করা (যদি থাকে)
                _suggestions.value = result.distinctBy { it.first }

            } catch (e: Exception) {
                // এরর হ্যান্ডলিং
                _errorMessage.value = "Failed to load suggestions: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // UI থেকে "Remove" বাটনে ক্লিক করলে লিস্ট থেকে সরিয়ে দেওয়া
    fun removeSuggestion(uid: String) {
        val currentList = _suggestions.value.toMutableList()
        currentList.removeAll { it.first == uid }
        _suggestions.value = currentList
    }

    // "Add Friend" বাটনে ক্লিক করলে রিকোয়েস্ট পাঠানো
    fun sendFriendRequest(uid: String) {
        viewModelScope.launch {
            try {
                // TODO: ফায়ারবেসে ফ্রেন্ড রিকোয়েস্ট পাঠানোর লজিক এখানে কল করবেন
                // উদাহরণ: repository.sendFriendRequest(currentUid, uid)

                // রিকোয়েস্ট সফল হলে লিস্ট থেকে ওই ইউজারকে সরিয়ে দেওয়া
                removeSuggestion(uid)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to send friend request: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // এরর ম্যাসেজ ক্লিয়ার করার জন্য
    fun clearError() {
        _errorMessage.value = null
    }

    // Suggestions রিফ্রেশ করার জন্য
    fun refreshSuggestions(currentUid: String) {
        _suggestions.value = emptyList()
        loadSuggestions(currentUid)
    }
}