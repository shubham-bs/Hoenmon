package com.homeenergymonitor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.util.formatClockTime
import com.homeenergymonitor.ui.components.ErrorCard
import com.homeenergymonitor.ui.components.formatNumber
import com.homeenergymonitor.ui.dashboard.HistoryUiState
import com.homeenergymonitor.ui.theme.Outline
import com.homeenergymonitor.ui.theme.Primary
import com.homeenergymonitor.ui.theme.SurfaceRaised
import com.homeenergymonitor.ui.theme.TextPrimary
import com.homeenergymonitor.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onLoadInitial: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onLoadInitial() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's readings",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${state.items.size} loaded",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onRefresh) {
                    Text("REFRESH", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.errorMessage != null) {
            item {
                ErrorCard(message = state.errorMessage, onRetry = onRefresh)
            }
        }

        items(items = state.items, key = { it.id }) { reading ->
            HistoryRow(reading)
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
                }
            }
        }

        if (!state.isLoading && state.hasLoaded && state.items.isEmpty()) {
            item {
                Text(
                    text = "No readings recorded today yet.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        }

        if (!state.isLast && state.items.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onLoadMore) {
                        Text("LOAD MORE", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(reading: MeterReading, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatClockTime(reading.timestamp),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatNumber(reading.voltage, 1)} V  |  ${formatNumber(reading.current, 2)} A",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatNumber(reading.activePowerKw, 2)} kW",
                    color = Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PF ${formatNumber(reading.powerFactor, 2)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
