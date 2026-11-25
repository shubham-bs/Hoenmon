package com.homeenergymonitor.ui.dashboard

import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import java.time.Instant

enum class LiveConnection {
    CONNECTING,
    LIVE,
    RECONNECTING,
    OFFLINE
}

data class DashboardUiState(
    val latestReading: MeterReading? = null,
    val status: MeterStatus? = null,
    val todaySummary: MeterSummary? = null,
    val connection: LiveConnection = LiveConnection.CONNECTING,
    val lastLiveUpdateAt: Instant? = null,
    val reconnectAttempt: Int = 0,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val demoData: Boolean = true
)
