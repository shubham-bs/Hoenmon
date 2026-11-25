package com.homeenergymonitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeenergymonitor.ui.theme.SurfaceVariant
import com.homeenergymonitor.ui.theme.TextPrimary
import com.homeenergymonitor.ui.theme.TextSecondary
import kotlin.math.min

private const val START_ANGLE = 140f
private const val SWEEP_ANGLE = 260f

@Composable
fun PowerGauge(
    powerKw: Double,
    maxKw: Double,
    accent: Color,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (powerKw / maxKw).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "gaugeProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.35f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.35f)) {
            val strokeWidth = 20.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth - 8.dp.toPx()
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = SurfaceVariant,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatNumber(powerKw, 2),
                color = TextPrimary,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "kW",
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            StatusPill(
                label = statusLabel,
                color = statusColor,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
