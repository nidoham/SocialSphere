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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.nidoham.social.user.User
import com.nidoham.social.user.UserExtractor
import com.nidoham.socialsphere.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OnboardActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userExtractor: UserExtractor

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
        userExtractor = UserExtractor(this)

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setContent {
            SocialSphereTheme {
                OnboardingScreen(
                    auth = auth,
                    userExtractor = userExtractor,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    auth: FirebaseAuth,
    userExtractor: UserExtractor,
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

    var isLogoVisible by remember { mutableStateOf(false) }
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoVisible = true
        delay(300)
        isContentVisible = true
    }

    suspend fun syncUserWithDatabase(firebaseUser: FirebaseUser) {
        try {
            // Check if user exists in Firestore/Cache
            val result = userExtractor.fetchCurrentUser(firebaseUser.uid)

            result.fold(
                onSuccess = { existingUser ->
                    if (existingUser == null) {
                        // CASE: New User - Create User Object
                        val newUser = User(
                            id = firebaseUser.uid,
                            username = firebaseUser.displayName?.replace(" ", "")?.lowercase()
                                ?: "user_${firebaseUser.uid.take(5)}",
                            name = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString(),
                            createdAt = System.currentTimeMillis(),
                            onlineAt = System.currentTimeMillis(),
                            verified = false,
                            privacy = User.Privacy.PUBLIC.value,
                            banned = false,
                            premium = false,
                            role = User.Role.USER.value
                        )
                        userExtractor.pushUser(newUser)
                        Log.d("OnboardActivity", "New user created: ${firebaseUser.uid}")
                    } else {
                        // CASE: Existing User - Update Last Login (onlineAt)
                        val updatedUser = existingUser.copy(
                            onlineAt = System.currentTimeMillis()
                        )
                        userExtractor.pushUser(updatedUser)
                        Log.d("OnboardActivity", "User login updated: ${firebaseUser.uid}")
                    }
                },
                onFailure = { e ->
                    Log.e("OnboardActivity", "Failed to check user existence", e)
                    // Fallback: Try to push a basic user object if fetch fails
                    val fallbackUser = User(
                        id = firebaseUser.uid,
                        username = "user_${firebaseUser.uid.take(5)}",
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        createdAt = System.currentTimeMillis(),
                        onlineAt = System.currentTimeMillis()
                    )
                    userExtractor.pushUser(fallbackUser)
                }
            )
        } catch (e: Exception) {
            Log.e("OnboardActivity", "Failed to sync user with database", e)
        }
    }

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

    // Instagram-inspired animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F0F), // Fallback DarkBackground
                        Color(0xFF1C212B),
                        Color(0xFF252933)
                    ),
                    startY = animatedOffset,
                    endY = animatedOffset + 1000f
                )
            )
    ) {
        // Instagram gradient overlay (subtle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF833AB4), // InstagramPurple
                            Color(0xFFC13584), // InstagramPink
                            Color(0xFFFD1D1D), // InstagramOrange
                            Color.Transparent
                        ),
                        radius = 1800f
                    )
                )
        )

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
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF833AB4),
                                        Color(0xFFC13584),
                                        Color(0xFFFD1D1D)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Fallback icon if drawable is missing, otherwise ensure R.drawable.app_icon exists
                        Icon(
                            imageVector = Icons.Default.Public, // Placeholder/Fallback
                            contentDescription = "SocialSphere",
                            modifier = Modifier.size(65.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "SocialSphere",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Connect with the world",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E) // DarkSurface
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign in to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Error Message
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = error,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = Color.Gray
                                )
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = "Password",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible)
                                            Icons.Outlined.Visibility
                                        else
                                            Icons.Outlined.VisibilityOff,
                                        contentDescription = if (isPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password",
                                        tint = Color.Gray
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Forgot Password
                        TextButton(
                            onClick = { handleForgotPassword() },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !isLoading
                        ) {
                            Text(
                                "Forgot Password?",
                                color = Color(0xFF2196F3), // LinkBlue
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = { handleEmailLogin() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    "Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        googleSignInClient?.let { client ->
                            Spacer(modifier = Modifier.height(28.dp))

                            // Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.DarkGray
                                )
                                Text(
                                    "OR",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.DarkGray
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Google Sign-In Button
                            OutlinedButton(
                                onClick = {
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF2C2C2E) // DarkCard
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.DarkGray
                                ),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Fallback Icon for Google if resource missing
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Google",
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Continue with Google",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Sign Up Link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Don't have an account? ",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Sign Up",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable {
                                    if (!isLoading) onNavigateToSignup()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        FloatingParticles()
    }
}

@Composable
fun FloatingParticles() {
    val particles = remember { List(10) { ParticleState() } }

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
                .alpha(0.2f)
                .clip(CircleShape)
                .background(particle.color)
        )
    }
}

data class ParticleState(
    val startX: Float = (0..350).random().toFloat(),
    val endX: Float = (0..350).random().toFloat(),
    val startY: Float = (0..900).random().toFloat(),
    val endY: Float = (0..900).random().toFloat(),
    val size: Int = (3..10).random(),
    val duration: Int = (4000..8000).random(),
    val color: Color = listOf(
        Color(0xFF833AB4), // InstagramPurple
        Color(0xFFC13584), // InstagramPink
        Color(0xFFFD1D1D), // InstagramOrange
        Color(0xFF2196F3), // Primary
        Color(0xFF03DAC6)  // Secondary
    ).random()
)