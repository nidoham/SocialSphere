package com.nidoham.socialsphere

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.nidoham.social.repository.UserRepository
import com.nidoham.socialsphere.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OnboardActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    // Pre-load webClientId OUTSIDE composable to avoid try-catch issues
    private val webClientId: String? by lazy {
        try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e("OnboardActivity", "Failed to load web client ID", e)
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setContent {
            SocialSphereTheme {
                OnboardingScreen(
                    auth = auth,
                    userRepository = userRepository,
                    webClientId = webClientId,
                    onLoginSuccess = { navigateToMain() },
                    onNavigateToSignup = {
                        Toast.makeText(this@OnboardActivity, "Signup coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun SocialSphereTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = if (isDark) darkColorScheme(
        primary = Color(0xFF6C63FF),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFFBB86FC),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
    ) else lightColorScheme(
        primary = Color(0xFF6C63FF),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFF9C27B0),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
    )

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    auth: FirebaseAuth,
    userRepository: UserRepository,
    webClientId: String?,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var isLogoVisible by remember { mutableStateOf(false) }
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoVisible = true
        delay(300)
        isContentVisible = true
    }

    // Sync user data with database after authentication
    suspend fun syncUserWithDatabase(firebaseUser: FirebaseUser) {
        try {
            val result = userRepository.getUserById(firebaseUser.uid)

            result.fold(
                onSuccess = { existingUser ->
                    if (existingUser == null) {
                        userRepository.createUser(firebaseUser)
                        Log.d("OnboardActivity", "New user created: ${firebaseUser.uid}")
                    } else {
                        userRepository.updateLastLogin(firebaseUser.uid)
                        Log.d("OnboardActivity", "User login updated: ${firebaseUser.uid}")
                    }
                },
                onFailure = { e ->
                    Log.e("OnboardActivity", "Failed to check user existence", e)
                    userRepository.createUser(firebaseUser)
                }
            )
        } catch (e: Exception) {
            Log.e("OnboardActivity", "Failed to sync user with database", e)
        }
    }

    // Google Sign-In setup - NO try-catch around composables!
    val googleSignInClient = remember(webClientId) {
        webClientId?.let { id ->
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(id)
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso)
            } catch (e: Exception) {
                Log.e("OnboardActivity", "Failed to initialize Google Sign-In", e)
                null
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()

                    authResult.user?.let { user ->
                        syncUserWithDatabase(user)
                    }

                    onLoginSuccess()
                } catch (e: Exception) {
                    Log.e("OnboardActivity", "Google sign-in failed", e)
                    errorMessage = "Google sign-in failed: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        } catch (e: ApiException) {
            Log.e("OnboardActivity", "Google sign-in API error", e)
            errorMessage = "Google sign-in cancelled or failed"
            isLoading = false
        }
    }

    fun handleEmailLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Please enter a valid email"
            return
        }

        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()

                authResult.user?.let { user ->
                    syncUserWithDatabase(user)
                }

                onLoginSuccess()
            } catch (e: Exception) {
                Log.e("OnboardActivity", "Email login failed", e)
                errorMessage = when {
                    e.message?.contains("password", ignoreCase = true) == true ->
                        "Invalid email or password"
                    e.message?.contains("user", ignoreCase = true) == true ->
                        "No account found with this email"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please try again"
                    else -> "Login failed. Please try again"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun handleForgotPassword() {
        if (email.isBlank()) {
            Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Toast.makeText(
                    context,
                    "Password reset email sent. Check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("OnboardActivity", "Password reset failed", e)
                Toast.makeText(context, "Failed to send reset email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF1a1a2e),
            Color(0xFF16213e),
            Color(0xFF0f3460)
        )
    } else {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors,
                    startY = animatedOffset,
                    endY = animatedOffset + 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Animated Logo
            AnimatedVisibility(
                visible = isLogoVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C63FF),
                                        Color(0xFF03DAC6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "SocialSphere",
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SocialSphere",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Connect with the world",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome Back",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign in to continue",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Error Message
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.3f
                                )
                            ),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password")
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible)
                                            Icons.Default.Visibility
                                        else
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handleEmailLogin()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.3f
                                )
                            ),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot Password
                        TextButton(
                            onClick = { handleForgotPassword() },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !isLoading
                        ) {
                            Text(
                                "Forgot Password?",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = { handleEmailLogin() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Google Sign-In section - Only if client is available
                        googleSignInClient?.let { client ->
                            Spacer(modifier = Modifier.height(24.dp))

                            // Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f))
                                Text(
                                    "OR",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Google Sign-In Button
                            OutlinedButton(
                                onClick = {
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isDark) Color(0xFF2C2C2C) else Color.White
                                ),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = "Google",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Continue with Google",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign Up Link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Don't have an account? ",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "Sign Up",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    if (!isLoading) onNavigateToSignup()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        FloatingParticles()
    }
}

@Composable
fun FloatingParticles() {
    val particles = remember { List(8) { ParticleState() } }

    particles.forEachIndexed { index, particle ->
        val infiniteTransition = rememberInfiniteTransition(label = "particle_$index")

        val offsetY by infiniteTransition.animateFloat(
            initialValue = particle.startY,
            targetValue = particle.endY,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "y_$index"
        )

        val offsetX by infiniteTransition.animateFloat(
            initialValue = particle.startX,
            targetValue = particle.endX,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.duration / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "x_$index"
        )

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size(particle.size.dp)
                .alpha(0.3f)
                .clip(CircleShape)
                .background(particle.color)
        )
    }
}

data class ParticleState(
    val startX: Float = (0..300).random().toFloat(),
    val endX: Float = (0..300).random().toFloat(),
    val startY: Float = (0..800).random().toFloat(),
    val endY: Float = (0..800).random().toFloat(),
    val size: Int = (4..12).random(),
    val duration: Int = (3000..6000).random(),
    val color: Color = listOf(
        Color(0xFF6C63FF),
        Color(0xFF03DAC6),
        Color(0xFFBB86FC),
        Color(0xFF9C27B0)
    ).random()
)
