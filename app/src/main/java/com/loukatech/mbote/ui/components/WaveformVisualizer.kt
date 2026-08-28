package com.loukatech.mbote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loukatech.mbote.ui.theme.PurpleLight
import com.loukatech.mbote.ui.theme.PurplePrimary
import kotlin.random.Random

@Composable
fun LiveRecordingWaveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
    waveColor: Color = PurplePrimary,
    barCount: Int = 28,
    maxBarHeight: Dp = 36.dp,
    minBarHeight: Dp = 4.dp
) {
    // Pulse phase animation for live recording feeling
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_factor"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxBarHeight)
            .testTag("live_waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val totalSpacingRatio = 0.4f
        val slotWidth = width / barCount
        val barWidth = slotWidth * (1f - totalSpacingRatio)
        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

        val paddedAmplitudes = if (amplitudes.size < barCount) {
            List(barCount - amplitudes.size) { 0.08f } + amplitudes
        } else {
            amplitudes.takeLast(barCount)
        }

        for (i in 0 until barCount) {
            val amp = paddedAmplitudes.getOrElse(i) { 0.08f }
            val rawFactor = if (isRecording && !isPaused) {
                val indexMod = 1f + 0.15f * kotlin.math.sin((i.toDouble() + (pulseFactor * 5)).toFloat())
                (amp * indexMod * pulseFactor).coerceIn(0.08f, 1.0f)
            } else if (isPaused) {
                (amp * 0.7f).coerceIn(0.08f, 1.0f)
            } else {
                0.08f
            }

            val barHeightPx = (rawFactor * height).coerceIn(minBarHeight.toPx(), height)
            val startX = i * slotWidth + (slotWidth - barWidth) / 2
            val startY = (height - barHeightPx) / 2

            val color = if (isPaused) {
                waveColor.copy(alpha = 0.5f)
            } else {
                val alpha = (0.4f + rawFactor * 0.6f).coerceIn(0.4f, 1.0f)
                waveColor.copy(alpha = alpha)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeightPx),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun AudioPlaybackWaveform(
    progress: Float,
    bars: List<Float> = remember { generateStaticWaveform(32) },
    activeColor: Color = PurplePrimary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    modifier: Modifier = Modifier,
    maxHeight: Dp = 24.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
            .testTag("playback_waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val barCount = bars.size
        val slotWidth = width / barCount
        val barWidth = (slotWidth * 0.65f).coerceAtLeast(2.dp.toPx())
        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

        val progressX = progress * width

        for (i in 0 until barCount) {
            val amp = bars.getOrElse(i) { 0.2f }.coerceIn(0.15f, 1.0f)
            val barHeightPx = (amp * height).coerceIn(4.dp.toPx(), height)
            val startX = i * slotWidth + (slotWidth - barWidth) / 2
            val startY = (height - barHeightPx) / 2

            val isPlayed = (startX + barWidth / 2) <= progressX
            val color = if (isPlayed) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeightPx),
                cornerRadius = cornerRadius
            )
        }
    }
}

fun generateStaticWaveform(count: Int = 30): List<Float> {
    val random = Random(42)
    return List(count) { i ->
        val wave = (kotlin.math.sin(i * 0.45) * 0.35 + 0.55).toFloat()
        val noise = (random.nextFloat() * 0.3f - 0.15f)
        (wave + noise).coerceIn(0.15f, 1.0f)
    }
}
