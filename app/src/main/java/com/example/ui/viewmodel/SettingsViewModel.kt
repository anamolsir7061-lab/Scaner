package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AnimationIntensity
import com.example.data.repository.DetectionMode
import com.example.data.repository.MarkerStyle
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.UserPreferences
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch { preferencesRepository.setLanguageCode(code) }
    }

    fun setDetectionMode(mode: DetectionMode) {
        viewModelScope.launch { preferencesRepository.setDetectionMode(mode) }
    }

    fun setMarkerStyle(style: MarkerStyle) {
        viewModelScope.launch { preferencesRepository.setMarkerStyle(style) }
    }

    fun setMarkerColorArgb(colorArgb: Long) {
        viewModelScope.launch { preferencesRepository.setMarkerColorArgb(colorArgb) }
    }

    fun setAnimationIntensity(intensity: AnimationIntensity) {
        viewModelScope.launch { preferencesRepository.setAnimationIntensity(intensity) }
    }

    fun setAutoOpenLink(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoOpenLink(enabled) }
    }

    fun setBeepEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBeepEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun setAutoTorchSuggestion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoTorchSuggestion(enabled) }
    }

    fun setAutoZoomEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoZoomEnabled(enabled) }
    }

    fun setShowTelemetry(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowTelemetry(enabled) }
    }

    class Factory(private val repository: PreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
