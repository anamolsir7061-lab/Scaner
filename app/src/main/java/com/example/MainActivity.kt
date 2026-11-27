package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.analyzer.BarcodeFormatHelper
import com.example.ui.components.AppLockScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InspectorScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AnamolScannerTheme
import com.example.ui.viewmodel.ScanHistoryViewModel
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class MainTab {
    SCAN,
    HISTORY,
    SETTINGS
}

class MainActivity : FragmentActivity() {

    private var initialLaunchMode by mutableStateOf<String?>(null)
    private var sharedImageUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AnamolApplication
        val initialPrefs = runBlocking { app.preferencesRepository.preferencesFlow.first() }
        app.applyLanguage(initialPrefs.languageCode)

        handleIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.preferencesRepository)
            )
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            var isLocked by remember { mutableStateOf(app.appLockManager.isLockEnabled()) }

            AnamolScannerTheme(themeMode = prefs.themeMode) {
                if (isLocked) {
                    val canBio = remember {
                        app.appLockManager.isBiometricEnabled() &&
                            app.appLockManager.canUseBiometrics(this)
                    }
                    val bioTitle = getString(R.string.biometric_prompt_title)
                    val bioSubtitle = getString(R.string.biometric_prompt_subtitle)
                    val cancelLabel = getString(R.string.cancel)

                    AppLockScreen(
                        biometricAvailable = canBio,
                        onRequestBiometric = {
                            app.appLockManager.showBiometricPrompt(
                                activity = this,
                                title = bioTitle,
                                subtitle = bioSubtitle,
                                negativeText = cancelLabel,
                                onSuccess = { isLocked = false },
                                onError = {}
                            )
                        },
                        onVerifyPin = { pin -> app.appLockManager.verifyPin(pin) },
                        onUnlocked = { isLocked = false }
                    )
                } else {
                    AnamolQrScannerRoot(
                        app = app,
                        settingsViewModel = settingsViewModel,
                        initialLaunchMode = initialLaunchMode,
                        sharedImageUri = sharedImageUri,
                        onClearSharedImage = { sharedImageUri = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        initialLaunchMode = intent.getStringExtra("LAUNCH_MODE")
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            sharedImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }
    }
}

@Composable
private fun AnamolQrScannerRoot(
    app: AnamolApplication,
    settingsViewModel: SettingsViewModel,
    initialLaunchMode: String?,
    sharedImageUri: Uri?,
    onClearSharedImage: () -> Unit
) {
    val context = LocalContext.current
    val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

    val scannerViewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModel.Factory(app.scanRepository)
    )
    val historyViewModel: ScanHistoryViewModel = viewModel(
        factory = ScanHistoryViewModel.Factory(app.scanRepository)
    )

    var currentTab by remember { mutableStateOf(MainTab.SCAN) }
    var inspectTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(initialLaunchMode) {
        when (initialLaunchMode) {
            "HISTORY" -> currentTab = MainTab.HISTORY
            "SCAN" -> currentTab = MainTab.SCAN
        }
    }

    // Handle external image shared into Anamol Scanner to decode QR/barcode
    LaunchedEffect(sharedImageUri) {
        val uri = sharedImageUri ?: return@LaunchedEffect
        try {
            val input = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
            ).process(input)
                .addOnSuccessListener { codes ->
                    val first = codes.firstOrNull()
                    val raw = first?.rawValue ?: first?.displayValue ?: ""
                    if (raw.isNotBlank() && first != null) {
                        currentTab = MainTab.SCAN
                        inspectTarget = null
                        val fmt = BarcodeFormatHelper.getFormatInfo(first.format).formatName
                        app.playScanFeedback(prefs.vibrationEnabled, prefs.beepEnabled)
                        scannerViewModel.decodeFromGallery(raw, fmt)
                    } else {
                        Toast.makeText(context, context.getString(R.string.no_code_in_image), Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (_: Exception) {}
        onClearSharedImage()
    }

    BackHandler(enabled = inspectTarget != null) {
        inspectTarget = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (inspectTarget == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.SCAN,
                        onClick = { currentTab = MainTab.SCAN },
                        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_scan)) },
                        modifier = Modifier.testTag("nav_tab_scan")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.HISTORY,
                        onClick = { currentTab = MainTab.HISTORY },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_history)) },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val target = inspectTarget
            if (target != null) {
                InspectorScreen(
                    rawContent = target.first,
                    format = target.second,
                    onBack = { inspectTarget = null }
                )
            } else {
                when (currentTab) {
                    MainTab.SCAN -> {
                        ScanScreen(
                            viewModel = scannerViewModel,
                            preferences = prefs,
                            onToggleDetectionMode = { mode ->
                                settingsViewModel.setDetectionMode(mode)
                            },
                            onInspectHex = { raw, format ->
                                inspectTarget = raw to format
                            }
                        )
                    }

                    MainTab.HISTORY -> {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onInspectItem = { raw, format ->
                                inspectTarget = raw to format
                            }
                        )
                    }

                    MainTab.SETTINGS -> {
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
