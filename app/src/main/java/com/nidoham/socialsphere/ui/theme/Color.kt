package com.nidoham.socialsphere.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color


/**
 * Light color scheme for the app.
 * This is the default color scheme used in the app.
 */
val lightColorScheme = lightColorScheme(
    primary = Color(0xFF2E3440),
    background = Color(0xFF2E3440),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFECEFF4)
)

/**
 * Dark color scheme for the app.
 * This color scheme is used when the app is in dark mode.
 */
val darkColorScheme = darkColorScheme(
    primary = Color(0xFF2E3440),
    background = Color(0xFF2E3440),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFECEFF4)
)