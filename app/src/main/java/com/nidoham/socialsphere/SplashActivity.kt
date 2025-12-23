package com.nidoham.socialsphere

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nidoham.socialsphere.auth.activities.LoginActivity
import com.nidoham.socialsphere.ui.theme.SocialSphereTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SocialSphereTheme {
                SplashScreen(
                    onTimeout = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    var logoVisible by remember { mutableStateOf(false) }
    var gradientVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        gradientVisible = true
        delay(200)
        logoVisible = true
        delay(2000) // Total splash duration: 2.3 seconds
        onTimeout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Instagram-inspired gradient background (subtle)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (gradientVisible) 0.15f else 0f)
                    .background(MaterialTheme.colorScheme.background)
            )

            // App logo with animation
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + scaleIn(
                    initialScale = 0.7f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "SocialSphere Logo",
                    modifier = Modifier.size(180.dp)
                )
            }
        }
    }
}