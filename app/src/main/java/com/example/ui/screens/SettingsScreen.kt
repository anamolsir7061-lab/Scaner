package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AnamolApplication
import com.example.R
import com.example.data.repository.AnimationIntensity
import com.example.data.repository.DetectionMode
import com.example.data.repository.MarkerStyle
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.SettingsViewModel

private val MARKER_COLORS = listOf(
    0xFF00F0FFL, // Neon Cyan
    0xFF00E5A3L, // Matrix Green
    0xFFFF2A55L, // Cyber Red
    0xFFA855F7L, // Laser Violet
    0xFFF59E0BL  // Solar Amber
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as? AnamolApplication ?: return
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    var lockEnabled by remember { mutableStateOf(app.appLockManager.isLockEnabled()) }
    var biometricEnabled by remember { mutableStateOf(app.appLockManager.isBiometricEnabled()) }
    val canBiometrics = remember { app.appLockManager.canUseBiometrics(context) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Detection & Cyber Marker
        SectionLabel(icon = Icons.Rounded.QrCodeScanner, text = stringResource(R.string.section_scanner_prefs))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.pref_detection_mode), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = prefs.detectionMode == DetectionMode.AUTO_MARKER,
                        onClick = { viewModel.setDetectionMode(DetectionMode.AUTO_MARKER) },
                        label = { Text(stringResource(R.string.detection_mode_auto)) },
                        modifier = Modifier.testTag("detection_auto")
                    )
                    FilterChip(
                        selected = prefs.detectionMode == DetectionMode.FIXED_CENTER,
                        onClick = { viewModel.setDetectionMode(DetectionMode.FIXED_CENTER) },
                        label = { Text(stringResource(R.string.detection_mode_center)) },
                        modifier = Modifier.testTag("detection_center")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(stringResource(R.string.pref_marker_style), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MarkerStyle.SQUARE to stringResource(R.string.marker_square),
                        MarkerStyle.CIRCLE to stringResource(R.string.marker_circle),
                        MarkerStyle.BRACKETS to stringResource(R.string.marker_brackets)
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = prefs.markerStyle == style,
                            onClick = { viewModel.setMarkerStyle(style) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("marker_${style.key}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.pref_marker_color), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MARKER_COLORS.forEach { argb ->
                        val selected = prefs.markerColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setMarkerColorArgb(argb) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(stringResource(R.string.pref_animation), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AnimationIntensity.OFF to stringResource(R.string.anim_off),
                        AnimationIntensity.NORMAL to stringResource(R.string.anim_normal),
                        AnimationIntensity.EXTREME to stringResource(R.string.anim_extreme)
                    ).forEach { (level, label) ->
                        FilterChip(
                            selected = prefs.animationIntensity == level,
                            onClick = { viewModel.setAnimationIntensity(level) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("anim_${level.key}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 2: Speed, Feedback & Automation Toggles
        SectionLabel(icon = Icons.Rounded.AutoAwesome, text = stringResource(R.string.section_behavior))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ToggleRow(
                    title = stringResource(R.string.pref_auto_open_link),
                    subtitle = stringResource(R.string.pref_auto_open_link_desc),
                    checked = prefs.autoOpenLink,
                    onCheckedChange = { viewModel.setAutoOpenLink(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.pref_beep),
                    subtitle = stringResource(R.string.pref_beep_desc),
                    checked = prefs.beepEnabled,
                    onCheckedChange = { viewModel.setBeepEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.pref_vibration),
                    subtitle = stringResource(R.string.pref_vibration_desc),
                    checked = prefs.vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.pref_auto_torch),
                    subtitle = stringResource(R.string.pref_auto_torch_desc),
                    checked = prefs.autoTorchSuggestion,
                    onCheckedChange = { viewModel.setAutoTorchSuggestion(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.pref_auto_zoom),
                    subtitle = stringResource(R.string.pref_auto_zoom_desc),
                    checked = prefs.autoZoomEnabled,
                    onCheckedChange = { viewModel.setAutoZoomEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.pref_telemetry),
                    subtitle = stringResource(R.string.pref_telemetry_desc),
                    checked = prefs.showTelemetryOverlay,
                    onCheckedChange = { viewModel.setShowTelemetry(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 3: Appearance & Language
        SectionLabel(icon = Icons.Rounded.Palette, text = stringResource(R.string.section_appearance))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        AppThemeMode.DARK to stringResource(R.string.theme_dark),
                        AppThemeMode.LIGHT to stringResource(R.string.theme_light)
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("theme_${mode.key}")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "en" to stringResource(R.string.lang_english),
                        "hi" to stringResource(R.string.lang_hindi)
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = prefs.languageCode == code,
                            onClick = {
                                viewModel.setLanguageCode(code)
                                app.applyLanguage(code)
                                (context as? Activity)?.recreate()
                            },
                            leadingIcon = if (prefs.languageCode == code) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(label) },
                            modifier = Modifier.testTag("lang_$code")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 4: Privacy & Security
        SectionLabel(icon = Icons.Rounded.Security, text = stringResource(R.string.section_security))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.encryption_badge_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.encryption_badge_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ToggleRow(
                    title = stringResource(R.string.app_lock_title),
                    subtitle = stringResource(R.string.app_lock_desc),
                    checked = lockEnabled,
                    onCheckedChange = { enable ->
                        if (enable && !app.appLockManager.hasPinConfigured()) {
                            pinInput = ""
                            showPinDialog = true
                        } else {
                            app.appLockManager.setLockEnabled(enable)
                            lockEnabled = enable
                        }
                    },
                    switchTag = "app_lock_switch"
                )

                if (lockEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            pinInput = ""
                            showPinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.change_pin))
                    }

                    if (canBiometrics) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(stringResource(R.string.biometric_unlock_title), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(R.string.biometric_unlock_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    app.appLockManager.setBiometricEnabled(it)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.set_pin_title)) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("4 digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            app.appLockManager.setPin(pinInput)
                            lockEnabled = true
                            showPinDialog = false
                            Toast.makeText(context, "Security PIN saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = pinInput.length == 4
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier
        )
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
