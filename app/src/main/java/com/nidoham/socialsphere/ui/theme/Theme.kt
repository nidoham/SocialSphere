package com.nidoham.socialsphere.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Custom shapes for the app
 * Optimized for social media UI elements
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Small badges, chips
    small = RoundedCornerShape(8.dp),        // Buttons, input fields
    medium = RoundedCornerShape(12.dp),      // Cards, dialogs
    large = RoundedCornerShape(16.dp),       // Bottom sheets, large cards
    extraLarge = RoundedCornerShape(28.dp)   // FABs, special components
)

/**
 * Main theme composable for SocialSphere app
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param content The content to be themed
 */
@Composable
fun SocialSphereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to true to enable Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color is available on Android 12+ (API 31+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Use custom color schemes
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // Set status bar icons color (dark icons for light theme, light icons for dark theme)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

/**
 * Preview theme for composable previews
 * Always uses light theme
 */
@Composable
fun SocialSpherePreviewTheme(
    content: @Composable () -> Unit
) {
    SocialSphereTheme(
        darkTheme = false,
        dynamicColor = false,
        content = content
    )
}