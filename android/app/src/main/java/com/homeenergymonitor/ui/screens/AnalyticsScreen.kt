package com.homeenergymonitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.util.formatClockTime
import com.homeenergymonitor.ui.components.MetricCard
import com.homeenergymonitor.ui.components.SectionCard
import com.homeenergymonitor.ui.components.formatNumber
import com.homeenergymonitor.ui.dashboard.DashboardUiState
import com.homeenergymonitor.ui.theme.Primary
import com.homeenergymonitor.ui.theme.TextPrimary
import com.homeenergymonitor.ui.theme.TextSecondary

@Composable
fun AnalyticsScreen(
    state: DashboardUiState,
    cachedReadings: List<MeterReading>,
    modifier: Modifier = Modifier
) {
    val summary = state.todaySummary
    val series = cachedReadings.take(40).reversed()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(
            title = "Today at a glance",
            subtitle = summary?.let {
                "${summary.readingCount} readings from ${formatClockTime(summary.from)}"
            } ?: "Waiting for today's summary"
        ) {
            SummaryGrid(
                metrics = listOf(
                    SummaryMetric("Energy in", summary?.let { formatNumber(it.energyImportedKwh, 3) } ?: "--", "kWh"),
                    SummaryMetric("Energy out", summary?.let { formatNumber(it.energyExportedKwh, 3) } ?: "--", "kWh"),
                    SummaryMetric("Average", summary?.let { formatNumber(it.averagePowerKw, 2) } ?: "--", "kW"),
                    SummaryMetric("Peak", summary?.let { formatNumber(it.peakPowerKw, 2) } ?: "--", "kW"),
                    SummaryMetric("Minimum", summary?.let { formatNumber(it.minPowerKw, 2) } ?: "--", "kW"),
                    SummaryMetric("Avg voltage", summary?.let { formatNumber(it.averageVoltage, 1) } ?: "--", "V"),
                    SummaryMetric("Avg current", summary?.let { formatNumber(it.averageCurrent, 2) } ?: "--", "A"),
                    SummaryMetric("Avg PF", summary?.let { formatNumber(it.averagePowerFactor, 3) } ?: "--", "")
                )
            )
        }

        SectionCard(
            title = "Power trend",
            subtitle = if (series.isEmpty()) {
                "No cached readings yet"
            } else {
                "Last ${series.size} cached readings"
            }
        ) {
            PowerBars(
                values = series.map { it.activePowerKw },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Voltage ${formatNumber(summary.minVoltage, 1)}-${formatNumber(summary.maxVoltage, 1)} V",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Avg freq ${formatNumber(summary.averageFrequency, 2)} Hz",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        SectionCard(
            title = "Insights",
            subtitle = "Derived from simulated telemetry"
        ) {
            val peak = summary?.peakPowerKw
            val avg = summary?.averagePowerKw
            Text(
                text = when {
                    peak == null || avg == null -> "Not enough data to produce insights yet."
                    peak > avg * 2 -> "Load is spiky: peak is more than twice the average demand."
                    avg > 2.0 -> "Household load is elevated; evening appliances may be running."
                    else -> "Load is steady and within a normal simulated range."
                },
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

private data class SummaryMetric(val label: String, val value: String, val unit: String)

@Composable
private fun SummaryGrid(metrics: List<SummaryMetric>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { metric ->
                    MetricCard(
                        label = metric.label,
                        value = metric.value,
                        unit = metric.unit,
                        modifier = Modifier.weight(1f),
                        accent = if (metric.value == "--") TextSecondary else TextPrimary
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PowerBars(values: List<Double>, modifier: Modifier = Modifier) {
    val maxValue = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size)
            .coerceAtLeast(1f)
        values.forEachIndexed { index, value ->
            val barHeight = ((value / maxValue).toFloat() * size.height)
                .coerceIn(2f, size.height)
            drawRoundRect(
                color = Primary.copy(alpha = 0.85f),
                topLeft = Offset(
                    x = index * (barWidth + gap),
                    y = size.height - barHeight
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
