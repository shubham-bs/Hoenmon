package com.homeenergymonitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reading: ReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(readings: List<ReadingEntity>)

    @Query("SELECT * FROM readings ORDER BY timestampEpochMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ReadingEntity>

    @Query("SELECT * FROM readings ORDER BY timestampEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReadingEntity>>

    @Query("DELETE FROM readings WHERE timestampEpochMillis < :cutoffEpochMillis")
    suspend fun deleteOlderThan(cutoffEpochMillis: Long)

    @Query("DELETE FROM readings")
    suspend fun clear()
}
