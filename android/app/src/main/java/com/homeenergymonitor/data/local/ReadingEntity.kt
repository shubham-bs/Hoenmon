package com.homeenergymonitor.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homeenergymonitor.domain.model.MeterReading
import java.time.Instant

@Entity(
    tableName = "readings",
    indices = [Index("timestampEpochMillis")]
)
data class ReadingEntity(
    @PrimaryKey val id: Long,
    val meterId: String,
    val timestampEpochMillis: Long,
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

fun MeterReading.toEntity(): ReadingEntity = ReadingEntity(
    id = id,
    meterId = meterId,
    timestampEpochMillis = timestamp.toEpochMilli(),
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

fun ReadingEntity.toDomain(): MeterReading = MeterReading(
    id = id,
    meterId = meterId,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
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
