package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AnamolApplication
import com.example.R
import com.example.analyzer.BarcodeFormatHelper
import com.example.analyzer.ParsedPayload
import com.example.analyzer.ThreatLevel
import com.example.data.repository.DetectionMode
import com.example.data.repository.UserPreferences
import com.example.scanner.DetectedBarcodeOverlay
import com.example.scanner.QrCoordinateMapper
import com.example.ui.components.QrOverlayCanvas
import com.example.ui.components.ScanResultSheet
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Size as AndroidSize

@Composable
fun ScanScreen(
    viewModel: ScannerViewModel,
    preferences: UserPreferences,
    onToggleDetectionMode: (DetectionMode) -> Unit,
    onInspectHex: (raw: String, format: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as? AnamolApplication
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessingFrame = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val currentPrefs by rememberUpdatedState(preferences)
    val isPaused by rememberUpdatedState(uiState.isScanningPaused)
    val markerColor = remember(preferences.markerColorArgb) { Color(preferences.markerColorArgb) }

    fun handleSelectedCode(overlay: DetectedBarcodeOverlay, decodeLatencyMs: Long) {
        app?.playScanFeedback(
            vibrate = currentPrefs.vibrationEnabled,
            beep = currentPrefs.beepEnabled
        )
        viewModel.selectAndDecodeBarcode(overlay, decodeLatencyMs)

        // Reset zoom gently after decode
        if (currentZoomRatio > 1.05f) {
            camera?.cameraControl?.setZoomRatio(1f)
            currentZoomRatio = 1f
        }
    }

    // Auto-open safe HTTPS links if user enabled Auto-Open Link setting
    LaunchedEffect(uiState.activeResult) {
        val result = uiState.activeResult ?: return@LaunchedEffect
        if (currentPrefs.autoOpenLink &&
            result.payload is ParsedPayload.Url &&
            result.threatLevel == ThreatLevel.SAFE
        ) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.payload.url)))
            } catch (_: Exception) {}
        }
    }

    // Rebind camera when lensFacing or permission changes
    LaunchedEffect(hasCameraPermission, lensFacing, previewViewRef) {
        val pv = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = pv.surfaceProvider
            }

            // Use 1280x720 target resolution for <30ms ML Kit frame analysis latency
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        AndroidSize(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
            )

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { proxy ->
                if (isPaused || !isProcessingFrame.compareAndSet(false, true)) {
                    proxy.close()
                    return@setAnalyzer
                }

                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    isProcessingFrame.set(false)
                    proxy.close()
                    return@setAnalyzer
                }

                val startNs = System.nanoTime()
                val yPlane = proxy.planes[0]
                val avgLuma = QrCoordinateMapper.computeAverageLuma(
                    yBuffer = yPlane.buffer,
                    width = proxy.width,
                    height = proxy.height,
                    rowStride = yPlane.rowStride
                )
                val lowLight = currentPrefs.autoTorchSuggestion && avgLuma < 45 && !isTorchOn

                val rotDeg = proxy.imageInfo.rotationDegrees
                val imgW = proxy.width
                val imgH = proxy.height
                val viewW = pv.width.toFloat().coerceAtLeast(1f)
                val viewH = pv.height.toFloat().coerceAtLeast(1f)
                val isFront = (lensFacing == CameraSelector.LENS_FACING_FRONT)

                val input = InputImage.fromMediaImage(mediaImage, rotDeg)
                barcodeScanner.process(input)
                    .addOnSuccessListener { codes ->
                        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                        val mapped = codes.mapNotNull { barcode ->
                            val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                            if (raw.isBlank()) return@mapNotNull null
                            val info = BarcodeFormatHelper.getFormatInfo(barcode.format)
                            QrCoordinateMapper.mapToNormalizedOverlay(
                                rawValue = raw,
                                formatName = info.formatName,
                                formatCode = barcode.format,
                                cornerPoints = barcode.cornerPoints,
                                boundingBox = barcode.boundingBox,
                                imageWidth = imgW,
                                imageHeight = imgH,
                                rotationDegrees = rotDeg,
                                viewWidth = viewW,
                                viewHeight = viewH,
                                isFrontCamera = isFront
                            )
                        }

                        val filtered = QrCoordinateMapper.filterByDetectionMode(
                            mapped,
                            currentPrefs.detectionMode
                        )

                        // Auto-zoom toward small/far codes if enabled
                        if (currentPrefs.autoZoomEnabled && filtered.size == 1) {
                            val target = filtered.first()
                            val nextZoom = QrCoordinateMapper.computeSuggestedZoom(
                                currentZoom = currentZoomRatio,
                                maxZoom = maxZoomRatio,
                                barcodeWidthRatio = target.boundingWidth,
                                barcodeHeightRatio = target.boundingHeight
                            )
                            if (nextZoom != null) {
                                currentZoomRatio = nextZoom
                                camera?.cameraControl?.setZoomRatio(nextZoom)
                            }
                        }

                        viewModel.updateTrackedFrame(
                            barcodes = filtered,
                            frameLatencyMs = elapsedMs,
                            isLowLight = lowLight
                        )

                        // Single code lock-on & decode immediately (<100ms)
                        if (filtered.size == 1 && !isPaused) {
                            val hit = filtered.first()
                            ContextCompat.getMainExecutor(context).execute {
                                handleSelectedCode(hit, elapsedMs)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        isProcessingFrame.set(false)
                        proxy.close()
                    }
            }

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis
                )
                camera = boundCamera
                val zoomState = boundCamera.cameraInfo.zoomState.value
                maxZoomRatio = zoomState?.maxZoomRatio ?: 4f
                currentZoomRatio = zoomState?.zoomRatio ?: 1f

                // Continuous center-weighted autofocus
                val factory = pv.meteringPointFactory
                val centerPoint = factory.createPoint(0.5f, 0.5f)
                val afAction = FocusMeteringAction.Builder(centerPoint)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                boundCamera.cameraControl.startFocusAndMetering(afAction)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    // Zero-permission Photo Picker for Gallery QR/Barcode Scan
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
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
                        val formatName = BarcodeFormatHelper.getFormatInfo(first.format).formatName
                        app?.playScanFeedback(
                            vibrate = preferences.vibrationEnabled,
                            beep = preferences.beepEnabled
                        )
                        viewModel.decodeFromGallery(raw, formatName)
                    } else {
                        Toast.makeText(context, context.getString(R.string.no_code_in_image), Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Decode error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = uiState.activeResult != null) {
        viewModel.dismissResult()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        detectTapGestures { offset ->
                            val pv = previewViewRef ?: return@detectTapGestures
                            val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                    }
            )

            // 60fps Live Corner-Point Tracking + Cyber Scan Effects Overlay
            QrOverlayCanvas(
                trackedBarcodes = uiState.trackedBarcodes,
                detectionMode = preferences.detectionMode,
                markerStyle = preferences.markerStyle,
                markerColor = markerColor,
                animationIntensity = preferences.animationIntensity,
                burstEvent = uiState.scanBurstEvent,
                onSelectBarcode = { selected ->
                    handleSelectedCode(selected, uiState.frameLatencyMs)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = markerColor,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("grant_camera_button")
                ) {
                    Text(stringResource(R.string.grant_camera))
                }
            }
        }

        // Top Cyber HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (preferences.showTelemetryOverlay && uiState.frameLatencyMs > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.frameLatencyMs}ms",
                            color = markerColor,
                            fontFamily = JetBrainsMonoFont,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val next = !isTorchOn
                        camera?.cameraControl?.enableTorch(next)
                        isTorchOn = next
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = stringResource(R.string.pref_torch),
                        tint = if (isTorchOn) markerColor else Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isTorchOn = false
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera),
                        tint = Color.White
                    )
                }
            }
        }

        // Guidance / Low-Light Torch Suggestion / Multi-QR Prompt
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = uiState.trackedBarcodes.size > 1) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.78f)
                ) {
                    Text(
                        text = stringResource(R.string.multi_qr_prompt, uiState.trackedBarcodes.size),
                        color = markerColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            AnimatedVisibility(visible = uiState.isLowLight && !isTorchOn) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier.clickable {
                        camera?.cameraControl?.enableTorch(true)
                        isTorchOn = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            tint = markerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.low_light_torch_hint),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Bottom Camera-First Control Pill Bar
        if (uiState.activeResult == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Scan Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("gallery_scan_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = markerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.import_gallery),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Detection Mode Toggle Pill
                val isAuto = preferences.detectionMode == DetectionMode.AUTO_MARKER
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            onToggleDetectionMode(
                                if (isAuto) DetectionMode.FIXED_CENTER else DetectionMode.AUTO_MARKER
                            )
                        }
                        .testTag("detection_mode_quick_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAuto) Icons.Rounded.TrackChanges else Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            tint = markerColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAuto) stringResource(R.string.mode_auto_short) else stringResource(R.string.mode_center_short),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Zoom Indicator / Reset Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            val next = if (currentZoomRatio > 1.8f) 1f else (currentZoomRatio + 0.75f).coerceAtMost(maxZoomRatio)
                            currentZoomRatio = next
                            camera?.cameraControl?.setZoomRatio(next)
                        }
                        .testTag("zoom_cycle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1fx", currentZoomRatio),
                            color = markerColor,
                            fontFamily = JetBrainsMonoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Spring Slide-Up Result Card
        AnimatedVisibility(
            visible = uiState.activeResult != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.activeResult?.let { report ->
                ScanResultSheet(
                    report = report,
                    onDismiss = { viewModel.dismissResult() },
                    onInspectHex = { raw, format ->
                        onInspectHex(raw, format)
                    }
                )
            }
        }
    }
}
