package com.nidoham.socialsphere.auth.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.*

class SignupViewModel : ViewModel() {

    // User data
    val firstName = mutableStateOf("")
    val lastName = mutableStateOf("")
    val username = mutableStateOf("")
    val profileImageUri = mutableStateOf<Uri?>(null)
    val birthday = mutableStateOf<Date?>(null)
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")

    // UI state
    val currentStep = mutableStateOf(SignupStep.PROFILE_AND_NAME)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val isAccountCreated = mutableStateOf(false)

    // Password visibility
    val isPasswordVisible = mutableStateOf(false)
    val isConfirmPasswordVisible = mutableStateOf(false)

    // Success callback
    private var onAccountCreatedCallback: (() -> Unit)? = null

    fun setOnAccountCreatedCallback(callback: () -> Unit) {
        onAccountCreatedCallback = callback
    }

    fun nextStep() {
        currentStep.value = when (currentStep.value) {
            SignupStep.PROFILE_AND_NAME -> SignupStep.BIRTHDAY
            SignupStep.BIRTHDAY -> SignupStep.EMAIL_AND_PASSWORD
            SignupStep.EMAIL_AND_PASSWORD -> SignupStep.EMAIL_AND_PASSWORD
        }
    }

    fun previousStep() {
        currentStep.value = when (currentStep.value) {
            SignupStep.PROFILE_AND_NAME -> SignupStep.PROFILE_AND_NAME
            SignupStep.BIRTHDAY -> SignupStep.PROFILE_AND_NAME
            SignupStep.EMAIL_AND_PASSWORD -> SignupStep.BIRTHDAY
        }
    }

    fun validateProfileAndName(): Boolean {
        return when {
            firstName.value.isBlank() -> {
                errorMessage.value = "Please enter your first name"
                false
            }
            firstName.value.length < 2 -> {
                errorMessage.value = "First name must be at least 2 characters"
                false
            }
            lastName.value.isBlank() -> {
                errorMessage.value = "Please enter your last name"
                false
            }
            lastName.value.length < 2 -> {
                errorMessage.value = "Last name must be at least 2 characters"
                false
            }
            username.value.isBlank() -> {
                errorMessage.value = "Please choose a username"
                false
            }
            username.value.length < 3 -> {
                errorMessage.value = "Username must be at least 3 characters"
                false
            }
            username.value.length > 30 -> {
                errorMessage.value = "Username must be less than 30 characters"
                false
            }
            !username.value.matches(Regex("^[a-z0-9._]+$")) -> {
                errorMessage.value = "Username can only contain lowercase letters, numbers, dots and underscores"
                false
            }
            username.value.startsWith(".") || username.value.endsWith(".") -> {
                errorMessage.value = "Username cannot start or end with a dot"
                false
            }
            else -> {
                errorMessage.value = null
                true
            }
        }
    }

    fun validateBirthday(): Boolean {
        return when {
            birthday.value == null -> {
                errorMessage.value = "Please select your birthday"
                false
            }
            !isAgeValid(birthday.value!!) -> {
                errorMessage.value = "You must be at least 13 years old to use this service"
                false
            }
            else -> {
                errorMessage.value = null
                true
            }
        }
    }

    fun validateEmailAndPassword(): Boolean {
        return when {
            email.value.isBlank() -> {
                errorMessage.value = "Please enter your email address"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches() -> {
                errorMessage.value = "Please enter a valid email address"
                false
            }
            password.value.isBlank() -> {
                errorMessage.value = "Please create a password"
                false
            }
            password.value.length < 6 -> {
                errorMessage.value = "Password must be at least 6 characters long"
                false
            }
            password.value.length > 128 -> {
                errorMessage.value = "Password must be less than 128 characters"
                false
            }
            confirmPassword.value.isBlank() -> {
                errorMessage.value = "Please confirm your password"
                false
            }
            password.value != confirmPassword.value -> {
                errorMessage.value = "Passwords do not match"
                false
            }
            else -> {
                errorMessage.value = null
                true
            }
        }
    }

    private fun isAgeValid(birthDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = birthDate

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age >= 13
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun createAccount() {
        if (!validateEmailAndPassword()) return

        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {
            try {
                // TODO: Replace with your actual API call
                // Example:
                // val imageUrl = profileImageUri.value?.let { uploadImage(it) }
                // val user = authRepository.createAccount(
                //     firstName = firstName.value,
                //     lastName = lastName.value,
                //     username = username.value,
                //     email = email.value,
                //     password = password.value,
                //     birthday = birthday.value!!,
                //     profileImageUrl = imageUrl
                // )

                // Simulate API call
                kotlinx.coroutines.delay(1500)

                // Success - mark account as created
                isAccountCreated.value = true
                onAccountCreatedCallback?.invoke()

            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to create account. Please try again."
            } finally {
                isLoading.value = false
            }
        }
    }
}

enum class SignupStep {
    PROFILE_AND_NAME,
    BIRTHDAY,
    EMAIL_AND_PASSWORD
}