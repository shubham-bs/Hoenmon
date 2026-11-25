package com.homeenergymonitor.data.remote.dto

import com.homeenergymonitor.domain.model.HistoryPage
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import java.time.Instant

data class MeterReadingDto(
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

data class MeterStatusDto(
    val meterId: String,
    val connectionState: String,
    val connected: Boolean,
    val source: String,
    val demo: Boolean,
    val lastReadingAt: Instant?,
    val message: String?
)

data class MeterSummaryDto(
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

data class PageResponseDto<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true
)

data class ApiErrorDto(
    val error: String?,
    val status: Int?,
    val message: String?,
    val source: String?,
    val demo: Boolean?,
    val timestamp: String?
)

fun MeterReadingDto.toDomain(): MeterReading = MeterReading(
    id = id,
    meterId = meterId,
    timestamp = timestamp,
    activePowerKw = activePowerKw,
    voltage = voltage,
    current = current,
    frequency = frequency,
    powerFactor = powerFactor,
    cumulativeImportKwh = cumulativeImportKwh,
    cumulativeExportKwh = cumulativeExportKwh,
    source = source,
    demo = demo
)

fun MeterStatusDto.toDomain(): MeterStatus = MeterStatus(
    meterId = meterId,
    connectionState = connectionState,
    connected = connected,
    source = source,
    demo = demo,
    lastReadingAt = lastReadingAt,
    message = message
)

fun MeterSummaryDto.toDomain(): MeterSummary = MeterSummary(
    from = from,
    to = to,
    readingCount = readingCount,
    energyImportedKwh = energyImportedKwh,
    energyExportedKwh = energyExportedKwh,
    averagePowerKw = averagePowerKw,
    peakPowerKw = peakPowerKw,
    minPowerKw = minPowerKw,
    averageVoltage = averageVoltage,
    minVoltage = minVoltage,
    maxVoltage = maxVoltage,
    averageCurrent = averageCurrent,
    averagePowerFactor = averagePowerFactor,
    averageFrequency = averageFrequency,
    source = source,
    demo = demo
)

fun PageResponseDto<MeterReadingDto>.toDomain(): HistoryPage = HistoryPage(
    content = content.map(MeterReadingDto::toDomain),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last
)
