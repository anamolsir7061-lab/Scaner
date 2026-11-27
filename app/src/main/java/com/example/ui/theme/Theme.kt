package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF002117),
    primaryContainer = EmeraldPrimaryDark.copy(alpha = 0.18f),
    onPrimaryContainer = EmeraldPrimaryDark,
    secondary = CyanAccentDark,
    onSecondary = Color(0xFF001E2E),
    secondaryContainer = CyanAccentDark.copy(alpha = 0.16f),
    onSecondaryContainer = CyanAccentDark,
    tertiary = LaserViolet,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.6f),
    error = StatusDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryLight.copy(alpha = 0.12f),
    onPrimaryContainer = EmeraldPrimaryLight,
    secondary = CyanAccentLight,
    onSecondary = Color.White,
    secondaryContainer = CyanAccentLight.copy(alpha = 0.12f),
    onSecondaryContainer = CyanAccentLight,
    tertiary = LaserViolet,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.7f),
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun AnamolScannerTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
