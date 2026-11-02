package com.bertschell.immichcontacts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Frame-synced arc spinner: rotates with withFrameNanos, i.e. once per display
 * frame (~16.7ms at 60Hz). No fixed timestep, so it can't stutter no matter
 * what the system animation scale or main-thread load is.
 */
@Composable
fun PulsingLoader(size: Dp = 48.dp) {
    val color = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val timeNanos = remember { mutableLongStateOf(0L) }

    remember(size) {
        scope.launch {
            while (isActive) {
                withFrameNanos { it -> timeNanos.longValue = it }
            }
        }
        true
    }

    Box(modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val tSec = timeNanos.longValue / 1_000_000_000.0
            val baseAngle = (tSec * 360.0) % 360.0          // full rotation per second
            val sweep = 90.0f
            drawArc(
                color = color,
                startAngle = baseAngle.toFloat(),
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = this.size.minDimension * 0.09f, cap = StrokeCap.Round),
            )
            // trailing short arc for a "comet" look
            drawArc(
                color = color.copy(alpha = 0.35f),
                startAngle = baseAngle.toFloat() - 30f,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(width = this.size.minDimension * 0.09f, cap = StrokeCap.Round),
            )
        }
    }
}
