package com.homeenergymonitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HomeEnergyDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}
