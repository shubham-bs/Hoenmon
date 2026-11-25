package com.homeenergymonitor

import com.homeenergymonitor.domain.model.HistoryPage
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import java.time.Instant

fun sampleReading(
    id: Long = 1L,
    timestamp: Instant = Instant.parse("2026-01-02T03:04:05Z"),
    activePowerKw: Double = 1.234
): MeterReading = MeterReading(
    id = id,
    meterId = "SIM-0001",
    timestamp = timestamp,
    activePowerKw = activePowerKw,
    voltage = 230.123,
    current = 5.4321,
    frequency = 50.012,
    powerFactor = 0.9512,
    cumulativeImportKwh = 12.345678,
    cumulativeExportKwh = 0.0,
    source = "SIMULATED",
    demo = true
)

fun sampleStatus(): MeterStatus = MeterStatus(
    meterId = "SIM-0001",
    connectionState = "CONNECTED",
    connected = true,
    source = "SIMULATED",
    demo = true,
    lastReadingAt = Instant.parse("2026-01-02T03:04:05Z"),
    message = "Simulated meter connected (DEMO DATA)"
)

fun sampleSummary(): MeterSummary = MeterSummary(
    from = Instant.parse("2026-01-02T00:00:00Z"),
    to = Instant.parse("2026-01-02T12:00:00Z"),
    readingCount = 42,
    energyImportedKwh = 8.5,
    energyExportedKwh = 0.0,
    averagePowerKw = 1.1,
    peakPowerKw = 4.2,
    minPowerKw = 0.1,
    averageVoltage = 229.8,
    minVoltage = 224.0,
    maxVoltage = 232.0,
    averageCurrent = 4.8,
    averagePowerFactor = 0.94,
    averageFrequency = 50.0,
    source = "SIMULATED",
    demo = true
)

fun sampleHistoryPage(
    page: Int = 0,
    last: Boolean = true,
    readings: List<MeterReading> = listOf(sampleReading())
): HistoryPage = HistoryPage(
    content = readings,
    page = page,
    size = 50,
    totalElements = readings.size.toLong(),
    totalPages = 1,
    first = page == 0,
    last = last
)

const val READING_JSON: String = """
{
  "id": 7,
  "meterId": "SIM-0001",
  "timestamp": "2026-01-02T03:04:05Z",
  "activePowerKw": 1.234,
  "voltage": 230.123,
  "current": 5.4321,
  "frequency": 50.012,
  "powerFactor": 0.9512,
  "cumulativeImportKwh": 12.345678,
  "cumulativeExportKwh": 0.0,
  "source": "SIMULATED",
  "demo": true
}
"""
