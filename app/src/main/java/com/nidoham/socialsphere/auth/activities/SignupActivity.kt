package com.nidoham.socialsphere.auth.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.nidoham.socialsphere.auth.screen.SignupNavigator
import com.nidoham.socialsphere.auth.viewmodel.SignupViewModel
import com.nidoham.socialsphere.ui.theme.SocialSphereTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth

        setContent {
            SocialSphereTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: SignupViewModel = viewModel()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    SignupNavigator(
                        viewModel = viewModel,
                        onBackPressed = { finish() },
                        onSignupComplete = {
                            scope.launch {
                                try {
                                    // Create user with email and password
                                    val result = auth.createUserWithEmailAndPassword(
                                        viewModel.email.value,
                                        viewModel.password.value
                                    ).await()

                                    // Update profile with username and additional info
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(viewModel.username.value)
                                        .build()

                                    result.user?.updateProfile(profileUpdates)?.await()

                                    // Send email verification
                                    result.user?.sendEmailVerification()?.await()

                                    Toast.makeText(
                                        context,
                                        "Account created successfully! Verification email sent.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Navigate to home or main activity
                                    navigateToHome()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Signup failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateToHome() {
        // TODO: Replace with your actual home activity
        // val intent = Intent(this, HomeActivity::class.java)
        // startActivity(intent)
        finish()
    }
}