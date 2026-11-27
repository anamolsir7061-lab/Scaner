package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Anamol Scanner Brand Tokens (Dark & Light Material 3)
val EmeraldPrimaryDark = Color(0xFF00E5A3)
val EmeraldPrimaryLight = Color(0xFF007E5D)
val CyanAccentDark = Color(0xFF38BDF8)
val CyanAccentLight = Color(0xFF0284C7)

// Dark Surfaces (Slate-Obsidian)
val DarkBackground = Color(0xFF090D14)
val DarkSurface = Color(0xFF101622)
val DarkSurfaceVariant = Color(0xFF172030)
val DarkOutline = Color(0xFF283548)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Surfaces (Crisp Paper Daylight)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F6)
val LightOutline = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

// Status & Semantic Tokens
val StatusSuccess = Color(0xFF10B981)
val StatusWarning = Color(0xFFF59E0B)
val StatusDanger = Color(0xFFEF4444)

// Legacy / Inspector Accent Aliases
val OledBlack = Color(0xFF000000)
val CyberBackground = DarkBackground
val CyberSurface = DarkSurface
val CyberCard = DarkSurfaceVariant
val CyberCardBorder = DarkOutline
val CyberTextPrimary = DarkTextPrimary
val CyberTextSecondary = DarkTextSecondary
val CyberTextMuted = Color(0xFF64748B)
val NeonCyan = Color(0xFF38BDF8)
val MatrixGreen = Color(0xFF00E5A3)
val LaserViolet = Color(0xFFA855F7)
val SolarAmber = Color(0xFFF59E0B)
val NeonCrimson = Color(0xFFEF4444)
val ThreatSafe = Color(0xFF10B981)
val ThreatSafeBg = Color(0xFF064E3B)
val ThreatWarning = Color(0xFFF59E0B)
val ThreatWarningBg = Color(0xFF451A03)
val ThreatDanger = Color(0xFFEF4444)
val ThreatDangerBg = Color(0xFF450A0A)

enum class AppThemeMode(val key: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}
