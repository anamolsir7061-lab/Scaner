package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analyzer.HackerDataInspector
import com.example.analyzer.InspectionReport
import com.example.data.repository.ScanRepository
import com.example.scanner.DetectedBarcodeOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanBurstEvent(
    val eventId: Long,
    val centerX: Float,
    val centerY: Float
)

data class ScannerUiState(
    val trackedBarcodes: List<DetectedBarcodeOverlay> = emptyList(),
    val activeResult: InspectionReport? = null,
    val scanBurstEvent: ScanBurstEvent? = null,
    val frameLatencyMs: Long = 0L,
    val decodeLatencyMs: Long = 0L,
    val isLowLight: Boolean = false,
    val isScanningPaused: Boolean = false
)

class ScannerViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastRecordedRaw: String? = null
    private var lastRecordedAtMs: Long = 0L

    fun updateTrackedFrame(
        barcodes: List<DetectedBarcodeOverlay>,
        frameLatencyMs: Long,
        isLowLight: Boolean
    ) {
        _uiState.update {
            it.copy(
                trackedBarcodes = barcodes,
                frameLatencyMs = frameLatencyMs,
                isLowLight = isLowLight
            )
        }
    }

    /**
     * Locks onto a single barcode, triggers burst animation, records to encrypted Room history,
     * and opens the spring-animated result sheet.
     */
    fun selectAndDecodeBarcode(
        overlay: DetectedBarcodeOverlay,
        decodeLatencyMs: Long = 0L
    ) {
        val now = System.currentTimeMillis()
        val report = HackerDataInspector.inspect(overlay.rawValue, overlay.formatName)

        _uiState.update {
            it.copy(
                activeResult = report,
                isScanningPaused = true,
                decodeLatencyMs = decodeLatencyMs,
                scanBurstEvent = ScanBurstEvent(
                    eventId = now,
                    centerX = overlay.centerX,
                    centerY = overlay.centerY
                )
            )
        }

        if (lastRecordedRaw != overlay.rawValue || (now - lastRecordedAtMs) > 2500L) {
            lastRecordedRaw = overlay.rawValue
            lastRecordedAtMs = now
            viewModelScope.launch {
                scanRepository.recordScan(
                    rawContent = report.raw,
                    format = report.formatDisplayName,
                    parsedType = report.mimeType,
                    securityRating = report.threatLevel.name,
                    title = report.raw.take(40)
                )
            }
        }
    }

    fun decodeFromGallery(raw: String, formatName: String) {
        val report = HackerDataInspector.inspect(raw, formatName)
        val now = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                activeResult = report,
                isScanningPaused = true,
                scanBurstEvent = ScanBurstEvent(
                    eventId = now,
                    centerX = 0.5f,
                    centerY = 0.5f
                )
            )
        }
        viewModelScope.launch {
            scanRepository.recordScan(
                rawContent = report.raw,
                format = report.formatDisplayName,
                parsedType = report.mimeType,
                securityRating = report.threatLevel.name,
                title = report.raw.take(40)
            )
        }
    }

    fun dismissResult() {
        _uiState.update {
            it.copy(
                activeResult = null,
                isScanningPaused = false,
                trackedBarcodes = emptyList()
            )
        }
    }

    class Factory(
        private val scanRepository: ScanRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(scanRepository) as T
        }
    }
}
