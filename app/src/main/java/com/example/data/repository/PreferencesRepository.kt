package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anamol_qr_prefs")

enum class DetectionMode(val key: String) {
    AUTO_MARKER("auto_marker"),
    FIXED_CENTER("fixed_center");

    companion object {
        fun fromKey(key: String?): DetectionMode =
            entries.firstOrNull { it.key == key } ?: AUTO_MARKER
    }
}

enum class MarkerStyle(val key: String) {
    SQUARE("square"),
    CIRCLE("circle"),
    BRACKETS("brackets");

    companion object {
        fun fromKey(key: String?): MarkerStyle =
            entries.firstOrNull { it.key == key } ?: SQUARE
    }
}

enum class AnimationIntensity(val key: String) {
    OFF("off"),
    NORMAL("normal"),
    EXTREME("extreme");

    companion object {
        fun fromKey(key: String?): AnimationIntensity =
            entries.firstOrNull { it.key == key } ?: EXTREME
    }
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val languageCode: String = "en",
    val detectionMode: DetectionMode = DetectionMode.AUTO_MARKER,
    val markerStyle: MarkerStyle = MarkerStyle.SQUARE,
    val markerColorArgb: Long = 0xFF00F0FFL, // Neon Cyan default
    val animationIntensity: AnimationIntensity = AnimationIntensity.EXTREME,
    val autoOpenLink: Boolean = false,
    val beepEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoTorchSuggestion: Boolean = true,
    val autoZoomEnabled: Boolean = true,
    val showTelemetryOverlay: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DETECTION_MODE = stringPreferencesKey("detection_mode")
        val MARKER_STYLE = stringPreferencesKey("marker_style")
        val MARKER_COLOR = longPreferencesKey("marker_color")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val AUTO_OPEN_LINK = booleanPreferencesKey("auto_open_link")
        val BEEP_ENABLED = booleanPreferencesKey("beep_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val AUTO_TORCH_SUGGESTION = booleanPreferencesKey("auto_torch_suggestion")
        val AUTO_ZOOM_ENABLED = booleanPreferencesKey("auto_zoom_enabled")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            detectionMode = DetectionMode.fromKey(prefs[Keys.DETECTION_MODE]),
            markerStyle = MarkerStyle.fromKey(prefs[Keys.MARKER_STYLE]),
            markerColorArgb = prefs[Keys.MARKER_COLOR] ?: 0xFF00F0FFL,
            animationIntensity = AnimationIntensity.fromKey(prefs[Keys.ANIMATION_INTENSITY]),
            autoOpenLink = prefs[Keys.AUTO_OPEN_LINK] ?: false,
            beepEnabled = prefs[Keys.BEEP_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            autoTorchSuggestion = prefs[Keys.AUTO_TORCH_SUGGESTION] ?: true,
            autoZoomEnabled = prefs[Keys.AUTO_ZOOM_ENABLED] ?: true,
            showTelemetryOverlay = prefs[Keys.SHOW_TELEMETRY] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setLanguageCode(langCode: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = langCode }
    }

    suspend fun setDetectionMode(mode: DetectionMode) {
        context.dataStore.edit { it[Keys.DETECTION_MODE] = mode.key }
    }

    suspend fun setMarkerStyle(style: MarkerStyle) {
        context.dataStore.edit { it[Keys.MARKER_STYLE] = style.key }
    }

    suspend fun setMarkerColorArgb(colorArgb: Long) {
        context.dataStore.edit { it[Keys.MARKER_COLOR] = colorArgb }
    }

    suspend fun setAnimationIntensity(intensity: AnimationIntensity) {
        context.dataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.key }
    }

    suspend fun setAutoOpenLink(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_OPEN_LINK] = enabled }
    }

    suspend fun setBeepEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BEEP_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAutoTorchSuggestion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_TORCH_SUGGESTION] = enabled }
    }

    suspend fun setAutoZoomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ZOOM_ENABLED] = enabled }
    }

    suspend fun setShowTelemetry(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TELEMETRY] = enabled }
    }
}
