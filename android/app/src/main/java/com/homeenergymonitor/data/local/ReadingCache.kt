package com.homeenergymonitor.data.local

import com.homeenergymonitor.domain.model.MeterReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReadingCache {
    fun observeRecent(limit: Int = 120): Flow<List<MeterReading>>
    suspend fun store(reading: MeterReading)
    suspend fun storeAll(readings: List<MeterReading>)
    suspend fun prune(cutoffEpochMillis: Long)
}

class RoomReadingCache(
    private val dao: ReadingDao
) : ReadingCache {

    override fun observeRecent(limit: Int): Flow<List<MeterReading>> =
        dao.observeRecent(limit).map { entities -> entities.map(ReadingEntity::toDomain) }

    override suspend fun store(reading: MeterReading) {
        dao.upsert(reading.toEntity())
    }

    override suspend fun storeAll(readings: List<MeterReading>) {
        if (readings.isNotEmpty()) {
            dao.upsertAll(readings.map(MeterReading::toEntity))
        }
    }

    override suspend fun prune(cutoffEpochMillis: Long) {
        dao.deleteOlderThan(cutoffEpochMillis)
    }
}
