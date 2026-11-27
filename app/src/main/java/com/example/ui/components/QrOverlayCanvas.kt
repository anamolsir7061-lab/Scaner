package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AnimationIntensity
import com.example.data.repository.DetectionMode
import com.example.data.repository.MarkerStyle
import com.example.scanner.DetectedBarcodeOverlay
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.viewmodel.ScanBurstEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QrOverlayCanvas(
    trackedBarcodes: List<DetectedBarcodeOverlay>,
    detectionMode: DetectionMode,
    markerStyle: MarkerStyle,
    markerColor: Color,
    animationIntensity: AnimationIntensity,
    burstEvent: ScanBurstEvent?,
    onSelectBarcode: (DetectedBarcodeOverlay) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_overlay")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep_phase"
    )

    // Burst shockwave & particle explosion state on decode
    val burstProgress = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(burstEvent?.eventId) {
        if (burstEvent != null && animationIntensity != AnimationIntensity.OFF) {
            burstProgress.snapTo(0f)
            flashAlpha.snapTo(if (animationIntensity == AnimationIntensity.EXTREME) 0.42f else 0.18f)
            flashAlpha.animateTo(0f, animationSpec = tween(220))
            burstProgress.animateTo(1f, animationSpec = tween(480))
        }
    }

    // Smoothly track primary barcode center & box for 60fps corner-point lock-on
    val primary = trackedBarcodes.firstOrNull()
    val hasPrimary = primary != null
    val targetCx by animateFloatAsState(
        targetValue = primary?.centerX ?: 0.5f,
        animationSpec = spring(stiffness = 650f, dampingRatio = 0.85f),
        label = "cx"
    )
    val targetCy by animateFloatAsState(
        targetValue = primary?.centerY ?: 0.5f,
        animationSpec = spring(stiffness = 650f, dampingRatio = 0.85f),
        label = "cy"
    )
    val targetW by animateFloatAsState(
        targetValue = primary?.boundingWidth ?: 0.25f,
        animationSpec = spring(stiffness = 650f, dampingRatio = 0.85f),
        label = "bw"
    )
    val targetH by animateFloatAsState(
        targetValue = primary?.boundingHeight ?: 0.25f,
        animationSpec = spring(stiffness = 650f, dampingRatio = 0.85f),
        label = "bh"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Fixed Center Frame Reticle (only when Fixed Center mode is enabled)
            if (detectionMode == DetectionMode.FIXED_CENTER) {
                val frameSize = min(w, h) * 0.62f
                val left = (w - frameSize) / 2f
                val top = (h - frameSize) / 2f
                drawMarkerShape(
                    left = left,
                    top = top,
                    boxW = frameSize,
                    boxH = frameSize,
                    style = markerStyle,
                    color = markerColor.copy(alpha = 0.85f),
                    glowAlpha = 0.25f
                )
                if (animationIntensity != AnimationIntensity.OFF) {
                    val scanY = top + frameSize * sweepPhase
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, markerColor, Color.Transparent),
                            startX = left,
                            endX = left + frameSize
                        ),
                        start = Offset(left + 12f, scanY),
                        end = Offset(left + frameSize - 12f, scanY),
                        strokeWidth = 3.5f
                    )
                }
            }

            // 2. Smooth 60fps Tracking Marker for Detected QR/Barcode(s)
            if (hasPrimary && primary != null) {
                val pad = 16f
                val boxW = max(90f, targetW * w + pad * 2f)
                val boxH = max(90f, targetH * h + pad * 2f)
                val left = (targetCx * w) - boxW / 2f
                val top = (targetCy * h) - boxH / 2f

                // Draw corner-point polygon path for accurate tilt/perspective tracking
                if (primary.corners.size >= 4) {
                    val polyPath = Path().apply {
                        val pts = primary.corners.map { Offset(it.x * w, it.y * h) }
                        moveTo(pts[0].x, pts[0].y)
                        lineTo(pts[1].x, pts[1].y)
                        lineTo(pts[2].x, pts[2].y)
                        lineTo(pts[3].x, pts[3].y)
                        close()
                    }
                    drawPath(
                        path = polyPath,
                        color = markerColor.copy(alpha = 0.14f)
                    )
                }

                // Shrinking cyber lock-on ring + pulse
                if (animationIntensity != AnimationIntensity.OFF) {
                    val expand = (1f - pulsePhase) * if (animationIntensity == AnimationIntensity.EXTREME) 34f else 18f
                    drawMarkerShape(
                        left = left - expand / 2f,
                        top = top - expand / 2f,
                        boxW = boxW + expand,
                        boxH = boxH + expand,
                        style = markerStyle,
                        color = markerColor.copy(alpha = (1f - pulsePhase) * 0.45f),
                        glowAlpha = 0f,
                        strokeWidth = 2.5f
                    )

                    // Glitch/laser sweep inside tracked marker
                    if (animationIntensity == AnimationIntensity.EXTREME) {
                        val lineY = top + boxH * sweepPhase
                        drawLine(
                            color = markerColor,
                            start = Offset(left + 6f, lineY),
                            end = Offset(left + boxW - 6f, lineY),
                            strokeWidth = 3f
                        )
                    }
                }

                drawMarkerShape(
                    left = left,
                    top = top,
                    boxW = boxW,
                    boxH = boxH,
                    style = markerStyle,
                    color = markerColor,
                    glowAlpha = 0.35f,
                    strokeWidth = 5f
                )
            }

            // Additional tracked codes when multiple codes appear in frame
            if (trackedBarcodes.size > 1) {
                trackedBarcodes.drop(1).forEach { code ->
                    val bw = max(84f, code.boundingWidth * w + 24f)
                    val bh = max(84f, code.boundingHeight * h + 24f)
                    val l = code.centerX * w - bw / 2f
                    val t = code.centerY * h - bh / 2f
                    drawMarkerShape(
                        left = l,
                        top = t,
                        boxW = bw,
                        boxH = bh,
                        style = markerStyle,
                        color = markerColor.copy(alpha = 0.8f),
                        glowAlpha = 0.2f,
                        strokeWidth = 4f
                    )
                }
            }

            // 3. Successful Decode Cyber Effect: Flash + Shockwave Ripple + Particle Burst
            val bp = burstProgress.value
            if (burstEvent != null && bp < 0.99f && animationIntensity != AnimationIntensity.OFF) {
                val origin = Offset(burstEvent.centerX * w, burstEvent.centerY * h)
                val maxRadius = min(w, h) * 0.55f

                // Screen flash
                if (flashAlpha.value > 0.01f) {
                    drawRect(color = markerColor.copy(alpha = flashAlpha.value))
                }

                // Shockwave ring
                drawCircle(
                    color = markerColor.copy(alpha = (1f - bp) * 0.85f),
                    radius = 24f + maxRadius * bp,
                    center = origin,
                    style = Stroke(width = (1f - bp) * 10f + 2f)
                )

                // Particle burst (Extreme mode)
                if (animationIntensity == AnimationIntensity.EXTREME) {
                    val particleCount = 20
                    for (i in 0 until particleCount) {
                        val angle = (2.0 * PI * i / particleCount).toFloat()
                        val dist = 20f + maxRadius * 0.85f * bp
                        val px = origin.x + cos(angle) * dist
                        val py = origin.y + sin(angle) * dist
                        drawCircle(
                            color = markerColor.copy(alpha = (1f - bp)),
                            radius = (1f - bp) * 7f + 2f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        // Interactive tap chips when multiple QR/barcodes are detected simultaneously
        if (trackedBarcodes.size > 1) {
            trackedBarcodes.forEachIndexed { idx, code ->
                val cxPx = (code.centerX * widthPx).roundToInt()
                val cyPx = (code.centerY * heightPx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(cxPx - 44, cyPx - 20) }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .border(1.5.dp, markerColor, RoundedCornerShape(16.dp))
                        .clickable { onSelectBarcode(code) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${idx + 1} ${code.formatName}",
                        color = markerColor,
                        fontFamily = JetBrainsMonoFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMarkerShape(
    left: Float,
    top: Float,
    boxW: Float,
    boxH: Float,
    style: MarkerStyle,
    color: Color,
    glowAlpha: Float,
    strokeWidth: Float = 5f
) {
    when (style) {
        MarkerStyle.SQUARE -> {
            if (glowAlpha > 0f) {
                drawRoundRect(
                    color = color.copy(alpha = glowAlpha),
                    topLeft = Offset(left - 4f, top - 4f),
                    size = Size(boxW + 8f, boxH + 8f),
                    cornerRadius = CornerRadius(22f, 22f),
                    style = Stroke(width = strokeWidth + 8f)
                )
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = strokeWidth)
            )
        }

        MarkerStyle.CIRCLE -> {
            val radius = max(boxW, boxH) / 2f
            val center = Offset(left + boxW / 2f, top + boxH / 2f)
            if (glowAlpha > 0f) {
                drawCircle(
                    color = color.copy(alpha = glowAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth + 8f)
                )
            }
            drawCircle(
                color = color,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
        }

        MarkerStyle.BRACKETS -> {
            val len = min(boxW, boxH) * 0.28f
            val right = left + boxW
            val bottom = top + boxH
            val strokes = if (glowAlpha > 0f) listOf(
                color.copy(alpha = glowAlpha) to (strokeWidth + 7f),
                color to strokeWidth
            ) else listOf(color to strokeWidth)

            strokes.forEach { (c, sw) ->
                // TL
                drawLine(c, Offset(left, top), Offset(left + len, top), sw, StrokeCap.Round)
                drawLine(c, Offset(left, top), Offset(left, top + len), sw, StrokeCap.Round)
                // TR
                drawLine(c, Offset(right - len, top), Offset(right, top), sw, StrokeCap.Round)
                drawLine(c, Offset(right, top), Offset(right, top + len), sw, StrokeCap.Round)
                // BL
                drawLine(c, Offset(left, bottom - len), Offset(left, bottom), sw, StrokeCap.Round)
                drawLine(c, Offset(left, bottom), Offset(left + len, bottom), sw, StrokeCap.Round)
                // BR
                drawLine(c, Offset(right - len, bottom), Offset(right, bottom), sw, StrokeCap.Round)
                drawLine(c, Offset(right, bottom - len), Offset(right, bottom), sw, StrokeCap.Round)
            }
        }
    }
}
