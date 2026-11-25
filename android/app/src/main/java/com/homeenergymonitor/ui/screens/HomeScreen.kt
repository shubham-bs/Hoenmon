package com.homeenergymonitor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeenergymonitor.domain.util.formatClockTime
import com.homeenergymonitor.domain.util.formatRelativeTime
import com.homeenergymonitor.domain.util.isStale
import com.homeenergymonitor.ui.components.ErrorCard
import com.homeenergymonitor.ui.components.InfoRow
import com.homeenergymonitor.ui.components.MetricCard
import com.homeenergymonitor.ui.components.PowerGauge
import com.homeenergymonitor.ui.components.SectionCard
import com.homeenergymonitor.ui.components.StatusPill
import com.homeenergymonitor.ui.components.formatNumber
import com.homeenergymonitor.ui.components.rememberNow
import com.homeenergymonitor.ui.dashboard.DashboardUiState
import com.homeenergymonitor.ui.dashboard.LiveConnection
import com.homeenergymonitor.ui.theme.LiveGreen
import com.homeenergymonitor.ui.theme.Primary
import com.homeenergymonitor.ui.theme.Secondary
import com.homeenergymonitor.ui.theme.TextPrimary
import com.homeenergymonitor.ui.theme.TextSecondary
import com.homeenergymonitor.ui.theme.WarnAmber

@Composable
fun HomeScreen(
    state: DashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now by rememberNow()
    val reading = state.latestReading
    val summary = state.todaySummary
    val stale = isStale(state.lastLiveUpdateAt, now)

    val statusLabel: String
    val statusColor: androidx.compose.ui.graphics.Color
    when {
        state.connection == LiveConnection.RECONNECTING -> {
            statusLabel = "RECONNECTING"
            statusColor = WarnAmber
        }
        reading == null && state.connection == LiveConnection.CONNECTING -> {
            statusLabel = "CONNECTING"
            statusColor = Secondary
        }
        state.lastLiveUpdateAt == null -> {
            statusLabel = "WAITING"
            statusColor = Secondary
        }
        stale -> {
            statusLabel = "STALE"
            statusColor = WarnAmber
        }
        else -> {
            statusLabel = "LIVE"
            statusColor = LiveGreen
        }
    }

    val updatedLabel = state.lastLiveUpdateAt?.let {
        "Updated ${formatRelativeTime(it, now)}"
    } ?: "Waiting for first live reading"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.errorMessage != null && state.connection != LiveConnection.RECONNECTING) {
            ErrorCard(message = state.errorMessage, onRetry = onRetry)
        }

        SectionCard(
            title = "Live power",
            subtitle = "Active household demand"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reading?.meterId ?: "SIM-0001",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                StatusPill(label = statusLabel, color = statusColor)
            }

            PowerGauge(
                powerKw = reading?.activePowerKw ?: 0.0,
                maxKw = 12.0,
                accent = if (stale || state.connection != LiveConnection.LIVE) WarnAmber else Primary,
                statusLabel = if (reading == null) "NO DATA" else "${reading.source} METER",
                statusColor = if (reading == null) TextSecondary else statusColor
            )

            Text(
                text = updatedLabel,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.connection == LiveConnection.RECONNECTING) {
                Text(
                    text = "Reconnecting (attempt ${state.reconnectAttempt})...",
                    color = WarnAmber,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        MetricGrid(
            metrics = listOf(
                Metric("Voltage", reading?.let { formatNumber(it.voltage, 1) } ?: "--", "V"),
                Metric("Current", reading?.let { formatNumber(it.current, 2) } ?: "--", "A"),
                Metric("Frequency", reading?.let { formatNumber(it.frequency, 2) } ?: "--", "Hz"),
                Metric("Power factor", reading?.let { formatNumber(it.powerFactor, 3) } ?: "--", ""),
                Metric("Today's energy", summary?.let { formatNumber(it.energyImportedKwh, 2) } ?: "--", "kWh"),
                Metric("Peak today", summary?.let { formatNumber(it.peakPowerKw, 2) } ?: "--", "kW")
            )
        )

        SectionCard(
            title = "Today",
            subtitle = summary?.let { "Since ${formatClockTime(it.from)}" } ?: "No summary yet"
        ) {
            InfoRow("Imported", summary?.let { "${formatNumber(it.energyImportedKwh, 3)} kWh" } ?: "--")
            InfoRow("Exported", summary?.let { "${formatNumber(it.energyExportedKwh, 3)} kWh" } ?: "--")
            InfoRow("Average power", summary?.let { "${formatNumber(it.averagePowerKw, 2)} kW" } ?: "--")
            InfoRow("Minimum power", summary?.let { "${formatNumber(it.minPowerKw, 2)} kW" } ?: "--")
            InfoRow("Readings", summary?.readingCount?.toString() ?: "--")
        }

        SectionCard(title = "Meter status") {
            InfoRow("State", state.status?.connectionState ?: "--")
            InfoRow("Connected", state.status?.connected?.let { if (it) "Yes" else "No" } ?: "--")
            InfoRow("Source", state.status?.source ?: reading?.source ?: "--")
            InfoRow(
                "Last reading",
                state.status?.lastReadingAt?.let { formatClockTime(it) } ?: "--"
            )
            InfoRow("Message", state.status?.message ?: "--")
            InfoRow("Data", "DEMO (simulated)")
        }

        Text(
            text = "Simulated single-phase household meter. All values are DEMO DATA.",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
    }
}

private data class Metric(val label: String, val value: String, val unit: String)

@Composable
private fun MetricGrid(metrics: List<Metric>, modifier: Modifier = Modifier) {
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
