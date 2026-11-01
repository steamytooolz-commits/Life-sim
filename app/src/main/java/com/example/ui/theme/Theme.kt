package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SovereignDarkColorScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = Color(0xFF1A1500),
    primaryContainer = Color(0xFF382F10),
    onPrimaryContainer = SovereignGoldBright,
    secondary = CyanAccent,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0D323A),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = CrimsonAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A1214),
    onTertiaryContainer = Color(0xFFFFB3B3),
    background = CharcoalBackground,
    onBackground = SlateTextPrimary,
    surface = CharcoalSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = CharcoalBorder,
    outlineVariant = Color(0xFF202632),
    error = CrimsonLight,
    onError = Color.White
)

@Composable
fun SovereignTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SovereignDarkColorScheme,
        typography = SovereignTypography,
        content = content
    )
}
