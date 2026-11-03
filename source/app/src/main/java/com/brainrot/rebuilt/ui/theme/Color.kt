package com.brainrot.rebuilt.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CoralPrimary = Color(0xFFFF5A5F)
val GreenSuccess = Color(0xFF00A86B)
val OrangeWarning = Color(0xFFFF9800)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFAAAAAA)

val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    secondary = GreenSuccess,
    tertiary = OrangeWarning,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    secondary = GreenSuccess,
    tertiary = OrangeWarning,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9ECEF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF212529),
    onSurface = Color(0xFF212529)
)

