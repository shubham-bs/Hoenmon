package com.homeenergymonitor.domain.model

import java.time.Instant

data class MeterReading(
    val id: Long,
    val meterId: String,
    val timestamp: Instant,
    val activePowerKw: Double,
    val voltage: Double,
    val current: Double,
    val frequency: Double,
    val powerFactor: Double,
    val cumulativeImportKwh: Double,
    val cumulativeExportKwh: Double,
    val source: String,
    val demo: Boolean
)

data class MeterStatus(
    val meterId: String,
    val connectionState: String,
    val connected: Boolean,
    val source: String,
    val demo: Boolean,
    val lastReadingAt: Instant?,
    val message: String?
)

data class MeterSummary(
    val from: Instant,
    val to: Instant,
    val readingCount: Long,
    val energyImportedKwh: Double,
    val energyExportedKwh: Double,
    val averagePowerKw: Double,
    val peakPowerKw: Double,
    val minPowerKw: Double,
    val averageVoltage: Double,
    val minVoltage: Double,
    val maxVoltage: Double,
    val averageCurrent: Double,
    val averagePowerFactor: Double,
    val averageFrequency: Double,
    val source: String,
    val demo: Boolean
)

data class HistoryPage(
    val content: List<MeterReading>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)
