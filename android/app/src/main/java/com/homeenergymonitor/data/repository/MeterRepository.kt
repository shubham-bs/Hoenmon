package com.homeenergymonitor.data.repository

import com.homeenergymonitor.domain.model.HistoryPage
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import com.homeenergymonitor.domain.model.TelemetryEvent
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface MeterRepository {
    val cachedReadings: Flow<List<MeterReading>>

    fun telemetry(): Flow<TelemetryEvent>

    suspend fun current(): MeterReading?

    suspend fun status(): MeterStatus

    suspend fun summary(from: Instant, to: Instant): MeterSummary

    suspend fun history(from: Instant, to: Instant, page: Int, size: Int): HistoryPage
}
