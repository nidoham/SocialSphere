package com.nidoham.socialsphere.auth.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.nidoham.socialsphere.auth.page.BirthdayScreen
import com.nidoham.socialsphere.auth.page.EmailAndPasswordScreen
import com.nidoham.socialsphere.auth.page.ProfileAndNameScreen
import com.nidoham.socialsphere.auth.viewmodel.SignupStep
import com.nidoham.socialsphere.auth.viewmodel.SignupViewModel

@Composable
fun SignupNavigator(
    viewModel: SignupViewModel,
    onBackPressed: () -> Unit,
    onSignupComplete: () -> Unit
) {
    // Observe when account is created
    LaunchedEffect(viewModel.isAccountCreated.value) {
        if (viewModel.isAccountCreated.value) {
            onSignupComplete()
        }
    }

    when (viewModel.currentStep.value) {
        SignupStep.PROFILE_AND_NAME -> ProfileAndNameScreen(viewModel, onBackPressed)
        SignupStep.BIRTHDAY -> BirthdayScreen(viewModel)
        SignupStep.EMAIL_AND_PASSWORD -> EmailAndPasswordScreen(viewModel)
    }
}