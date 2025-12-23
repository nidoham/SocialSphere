package com.nidoham.socialsphere.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Light color scheme for the app.
 * Complete Material 3 color palette for day mode.
 */
val lightColorScheme = lightColorScheme(
    // Primary colors
    primary = Color(0xFF5E81AC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2F3),
    onPrimaryContainer = Color(0xFF001D36),

    // Secondary colors
    secondary = Color(0xFF88C0D0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E9F2),
    onSecondaryContainer = Color(0xFF001F28),

    // Tertiary colors
    tertiary = Color(0xFFB48EAD),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4E5F1),
    onTertiaryContainer = Color(0xFF2D1228),

    // Error colors
    error = Color(0xFFBF616A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Background colors
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),

    // Surface colors
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E4E9),
    onSurfaceVariant = Color(0xFF434C5E),
    surfaceTint = Color(0xFF5E81AC),

    // Inverse colors
    inverseSurface = Color(0xFF2F3133),
    inverseOnSurface = Color(0xFFF1F3F5),
    inversePrimary = Color(0xFFABC8E8),

    // Outline colors
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7CF),

    // Scrim
    scrim = Color(0xFF000000)
)

/**
 * Dark color scheme for the app.
 * Complete Material 3 color palette for night mode.
 */
val darkColorScheme = darkColorScheme(
    // Primary colors
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C6F),
    onPrimaryContainer = Color(0xFFBFE3F7),

    // Secondary colors
    secondary = Color(0xFF5E81AC),
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF1F4D67),
    onSecondaryContainer = Color(0xFFB8D9F0),

    // Tertiary colors
    tertiary = Color(0xFFD8B9D4),
    onTertiary = Color(0xFF3E2540),
    tertiaryContainer = Color(0xFF563B57),
    onTertiaryContainer = Color(0xFFF4E5F1),

    // Error colors
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Background colors - Keeping your original dark background
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFE5E9F0),

    // Surface colors
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFE5E9F0),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFD8DEE9),
    surfaceTint = Color(0xFF88C0D0),

    // Inverse colors
    inverseSurface = Color(0xFFE5E9F0),
    inverseOnSurface = Color(0xFF2E3440),
    inversePrimary = Color(0xFF00658E),

    // Outline colors
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF44474F),

    // Scrim
    scrim = Color(0xFF000000)
)