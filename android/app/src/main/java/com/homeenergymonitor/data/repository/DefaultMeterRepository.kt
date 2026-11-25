package com.homeenergymonitor.data.repository

import com.google.gson.Gson
import com.homeenergymonitor.data.local.ReadingCache
import com.homeenergymonitor.data.remote.MeterApi
import com.homeenergymonitor.data.remote.SseSignal
import com.homeenergymonitor.data.remote.SseTelemetryClient
import com.homeenergymonitor.data.remote.dto.MeterReadingDto
import com.homeenergymonitor.data.remote.dto.toDomain
import com.homeenergymonitor.domain.model.HistoryPage
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import com.homeenergymonitor.domain.model.TelemetryEvent
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

private const val MAX_CACHE_AGE_MILLIS = 24L * 60L * 60L * 1_000L
private const val CACHE_PRUNE_INTERVAL = 50

fun backoffDelayMs(attempt: Int): Long {
    if (attempt <= 0) return 1_000L
    val exponent = (attempt - 1).coerceAtMost(4)
    return (1_000L shl exponent).coerceAtMost(15_000L)
}

class DefaultMeterRepository(
    private val api: MeterApi,
    private val sseClient: SseTelemetryClient,
    private val cache: ReadingCache,
    private val gson: Gson
) : MeterRepository {

    override val cachedReadings: Flow<List<MeterReading>> =
        cache.observeRecent(120)

    override fun telemetry(): Flow<TelemetryEvent> = flow {
        var attempt = 0
        var cachedSincePrune = 0

        while (true) {
            emit(TelemetryEvent.Connecting)
            var connectedThisSession = false
            var lastFailure: String? = null

            sseClient.streamOnce().collect { signal ->
                when (signal) {
                    SseSignal.Opened -> {
                        connectedThisSession = true
                        attempt = 0
                        emit(TelemetryEvent.Connected(Instant.now()))
                    }

                    is SseSignal.Event -> {
                        if (signal.type == "reading") {
                            parseReading(signal.data)?.let { reading ->
                                cache.store(reading)
                                cachedSincePrune++
                                if (cachedSincePrune >= CACHE_PRUNE_INTERVAL) {
                                    cachedSincePrune = 0
                                    cache.prune(
                                        System.currentTimeMillis() - MAX_CACHE_AGE_MILLIS
                                    )
                                }
                                emit(TelemetryEvent.Reading(reading))
                            }
                        }
                    }

                    is SseSignal.Failure -> {
                        lastFailure = signal.message
                    }

                    SseSignal.Closed -> Unit
                }
            }

            attempt = if (connectedThisSession) 1 else attempt + 1
            val waitMillis = backoffDelayMs(attempt)
            emit(TelemetryEvent.Reconnecting(attempt, waitMillis, lastFailure))
            delay(waitMillis)
        }
    }

    override suspend fun current(): MeterReading? {
        val response = api.current()
        if (response.code() == 204) return null
        if (!response.isSuccessful) throw HttpException(response)
        return response.body()?.toDomain()
    }

    override suspend fun status(): MeterStatus = api.status().toDomain()

    override suspend fun summary(from: Instant, to: Instant): MeterSummary =
        api.summary(from.toString(), to.toString()).toDomain()

    override suspend fun history(
        from: Instant,
        to: Instant,
        page: Int,
        size: Int
    ): HistoryPage =
        api.history(from.toString(), to.toString(), page, size, "desc").toDomain()

    private fun parseReading(data: String): MeterReading? =
        try {
            gson.fromJson(data, MeterReadingDto::class.java)?.toDomain()
        } catch (error: Exception) {
            null
        }
}
