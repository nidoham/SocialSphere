package com.nidoham.socialsphere.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    // Primary colors (Instagram Purple)
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryLight,

    // Secondary colors (Instagram Orange)
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryLight,

    // Tertiary colors (Instagram Pink)
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryLight,

    // Background colors
    background = DarkBackground,
    onBackground = TextPrimary,

    // Surface colors
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,

    // Container colors
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkElevated,
    surfaceContainerLow = DarkSurface,
    surfaceContainerLowest = DarkBackground,

    // Inverse colors
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = PrimaryDark,

    // Error colors
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFB4AB),

    // Outline colors
    outline = BorderColor,
    outlineVariant = DividerColor,

    // Scrim
    scrim = Color.Black
)

@Composable
fun SocialSphereTheme(
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        // Set status bar color
        systemUiController.setStatusBarColor(
            color = StatusBar,
            darkIcons = false
        )

        // Set navigation bar color
        systemUiController.setNavigationBarColor(
            color = NavigationBar,
            darkIcons = false
        )
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}