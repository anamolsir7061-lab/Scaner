package com.example.scanner

import android.graphics.Point
import android.graphics.Rect
import com.example.data.repository.DetectionMode
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class DetectedBarcodeOverlay(
    val id: String,
    val rawValue: String,
    val formatName: String,
    val formatCode: Int,
    val corners: List<NormalizedPoint>,
    val centerX: Float,
    val centerY: Float,
    val boundingWidth: Float,
    val boundingHeight: Float
)

object QrCoordinateMapper {

    /**
     * Maps ML Kit image coordinates (accounting for camera rotation and FILL_CENTER preview crop)
     * into normalized [0f..1f] screen coordinates for smooth 60fps overlay tracking.
     */
    fun mapToNormalizedOverlay(
        rawValue: String,
        formatName: String,
        formatCode: Int,
        cornerPoints: Array<Point>?,
        boundingBox: Rect?,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        viewWidth: Float,
        viewHeight: Float,
        isFrontCamera: Boolean = false
    ): DetectedBarcodeOverlay {
        val rotatedW = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight.toFloat() else imageWidth.toFloat()
        val rotatedH = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth.toFloat() else imageHeight.toFloat()

        val safeRotW = max(1f, rotatedW)
        val safeRotH = max(1f, rotatedH)
        val safeViewW = max(1f, viewWidth)
        val safeViewH = max(1f, viewHeight)

        // PreviewView uses FILL_CENTER: scale uniformly by max(scaleX, scaleY) and center-crop overflow
        val scale = max(safeViewW / safeRotW, safeViewH / safeRotH)
        val scaledW = safeRotW * scale
        val scaledH = safeRotH * scale
        val offsetX = (safeViewW - scaledW) / 2f
        val offsetY = (safeViewH - scaledH) / 2f

        fun mapPoint(px: Float, py: Float): NormalizedPoint {
            val sx = ((px * scale + offsetX) / safeViewW).coerceIn(0f, 1f)
            val sy = ((py * scale + offsetY) / safeViewH).coerceIn(0f, 1f)
            val finalX = if (isFrontCamera) 1f - sx else sx
            return NormalizedPoint(finalX, sy)
        }

        val mappedCorners = when {
            cornerPoints != null && cornerPoints.size >= 4 -> {
                cornerPoints.take(4).map { mapPoint(it.x.toFloat(), it.y.toFloat()) }
            }
            boundingBox != null -> {
                listOf(
                    mapPoint(boundingBox.left.toFloat(), boundingBox.top.toFloat()),
                    mapPoint(boundingBox.right.toFloat(), boundingBox.top.toFloat()),
                    mapPoint(boundingBox.right.toFloat(), boundingBox.bottom.toFloat()),
                    mapPoint(boundingBox.left.toFloat(), boundingBox.bottom.toFloat())
                )
            }
            else -> listOf(
                NormalizedPoint(0.35f, 0.35f),
                NormalizedPoint(0.65f, 0.35f),
                NormalizedPoint(0.65f, 0.65f),
                NormalizedPoint(0.35f, 0.65f)
            )
        }

        val minX = mappedCorners.minOf { it.x }
        val maxX = mappedCorners.maxOf { it.x }
        val minY = mappedCorners.minOf { it.y }
        val maxY = mappedCorners.maxOf { it.y }

        return DetectedBarcodeOverlay(
            id = "${formatCode}_${rawValue.hashCode()}",
            rawValue = rawValue,
            formatName = formatName,
            formatCode = formatCode,
            corners = mappedCorners,
            centerX = (minX + maxX) / 2f,
            centerY = (minY + maxY) / 2f,
            boundingWidth = (maxX - minX).coerceAtLeast(0.04f),
            boundingHeight = (maxY - minY).coerceAtLeast(0.04f)
        )
    }

    fun filterByDetectionMode(
        overlays: List<DetectedBarcodeOverlay>,
        mode: DetectionMode
    ): List<DetectedBarcodeOverlay> {
        if (mode == DetectionMode.AUTO_MARKER) return overlays
        return overlays.filter { item ->
            item.centerX in 0.16f..0.84f && item.centerY in 0.24f..0.76f
        }
    }

    /**
     * Fast subsampled luma average (<0.3ms) to detect low-light conditions for auto-torch suggestion.
     */
    fun computeAverageLuma(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int
    ): Int {
        if (width <= 0 || height <= 0) return 128
        val limit = yBuffer.limit()
        val stepX = max(1, width / 16)
        val stepY = max(1, height / 16)
        var sum = 0L
        var count = 0
        var y = 0
        while (y < height) {
            val rowOffset = y * rowStride
            var x = 0
            while (x < width) {
                val idx = rowOffset + x
                if (idx in 0 until limit) {
                    sum += (yBuffer.get(idx).toInt() and 0xFF)
                    count++
                }
                x += stepX
            }
            y += stepY
        }
        return if (count == 0) 128 else (sum / count).toInt()
    }

    /**
     * Suggests progressive smooth zoom when a detected barcode is small/far in frame.
     */
    fun computeSuggestedZoom(
        currentZoom: Float,
        maxZoom: Float,
        barcodeWidthRatio: Float,
        barcodeHeightRatio: Float
    ): Float? {
        val dominant = max(barcodeWidthRatio, barcodeHeightRatio)
        if (dominant >= 0.16f || maxZoom <= 1.05f) return null
        val target = min(min(maxZoom, 3.5f), currentZoom + 0.22f)
        return if (target - currentZoom > 0.05f) target else null
    }
}
