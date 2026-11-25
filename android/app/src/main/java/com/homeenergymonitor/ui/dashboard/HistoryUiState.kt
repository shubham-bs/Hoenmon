package com.homeenergymonitor.ui.dashboard

import com.homeenergymonitor.domain.model.MeterReading

data class HistoryUiState(
    val items: List<MeterReading> = emptyList(),
    val page: Int = -1,
    val isLast: Boolean = false,
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null
)
