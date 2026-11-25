package com.homeenergymonitor.data.repository

import com.google.common.truth.Truth.assertThat
import com.homeenergymonitor.READING_JSON
import com.homeenergymonitor.data.local.ReadingCache
import com.homeenergymonitor.data.remote.MeterApi
import com.homeenergymonitor.data.remote.SseTelemetryClient
import com.homeenergymonitor.data.remote.buildGson
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.TelemetryEvent
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DefaultMeterRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var cache: RecordingCache
    private lateinit var repository: DefaultMeterRepository

    private val gson = buildGson()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cache = RecordingCache()

        val baseUrl = server.url("/").toString()
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MeterApi::class.java)

        val sseHttp = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        repository = DefaultMeterRepository(
            api = api,
            sseClient = SseTelemetryClient(sseHttp, baseUrl),
            cache = cache,
            gson = gson
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun backoffGrowsAndCaps() {
        assertThat(backoffDelayMs(0)).isEqualTo(1_000L)
        assertThat(backoffDelayMs(1)).isEqualTo(1_000L)
        assertThat(backoffDelayMs(2)).isEqualTo(2_000L)
        assertThat(backoffDelayMs(3)).isEqualTo(4_000L)
        assertThat(backoffDelayMs(4)).isEqualTo(8_000L)
        assertThat(backoffDelayMs(5)).isEqualTo(15_000L)
        assertThat(backoffDelayMs(20)).isEqualTo(15_000L)
    }

    @Test
    fun currentReturnsNullForNoContent() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        assertThat(repository.current()).isNull()
    }

    @Test
    fun mapsRestResponsesToDomain() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(READING_JSON)
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(STATUS_JSON)
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(SUMMARY_JSON)
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(HISTORY_JSON)
        )

        val current = repository.current()
        val status = repository.status()
        val summary = repository.summary(
            Instant.parse("2026-01-02T00:00:00Z"),
            Instant.parse("2026-01-02T12:00:00Z")
        )
        val history = repository.history(
            Instant.parse("2026-01-02T00:00:00Z"),
            Instant.parse("2026-01-02T12:00:00Z"),
            page = 0,
            size = 50
        )

        assertThat(current?.id).isEqualTo(7L)
        assertThat(status.meterId).isEqualTo("SIM-0001")
        assertThat(status.connected).isTrue()
        assertThat(summary.peakPowerKw).isEqualTo(4.2)
        assertThat(history.content).hasSize(1)
        assertThat(history.last).isTrue()

        assertThat(server.takeRequest().path).contains("/api/v1/meter/current")
        assertThat(server.takeRequest().path).contains("/api/v1/meter/status")
        val summaryPath = server.takeRequest().path.orEmpty()
        assertThat(summaryPath).contains("/api/v1/meter/summary")
        assertThat(summaryPath).contains("from=")
        assertThat(server.takeRequest().path).contains("/api/v1/meter/history")
    }

    @Test
    fun telemetryParsesReadingAndStoresInCache() = runBlocking {
        val compactReading = """
            {"id":7,"meterId":"SIM-0001","timestamp":"2026-01-02T03:04:05Z","activePowerKw":1.234,"voltage":230.123,"current":5.4321,"frequency":50.012,"powerFactor":0.9512,"cumulativeImportKwh":12.345678,"cumulativeExportKwh":0.0,"source":"SIMULATED","demo":true}
        """.trimIndent()
        val body = "event: connected\ndata: {\"type\":\"connected\"}\n\n" +
            "event: reading\nid: 7\ndata: $compactReading\n\n"
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body)
        )

        val readingEvent = withTimeout(5_000) {
            repository.telemetry().first { it is TelemetryEvent.Reading }
        } as TelemetryEvent.Reading

        assertThat(readingEvent.reading.id).isEqualTo(7L)
        assertThat(readingEvent.reading.activePowerKw).isEqualTo(1.234)
        assertThat(cache.stored).hasSize(1)
        assertThat(cache.stored.first().timestamp)
            .isEqualTo(Instant.parse("2026-01-02T03:04:05Z"))
    }

    private class RecordingCache : ReadingCache {
        val stored = mutableListOf<MeterReading>()
        private val flow = MutableStateFlow<List<MeterReading>>(emptyList())

        override fun observeRecent(limit: Int): Flow<List<MeterReading>> = flow

        override suspend fun store(reading: MeterReading) {
            stored += reading
        }

        override suspend fun storeAll(readings: List<MeterReading>) {
            stored += readings
        }

        override suspend fun prune(cutoffEpochMillis: Long) = Unit
    }

    private companion object {
        const val STATUS_JSON = """
        {
          "meterId": "SIM-0001",
          "connectionState": "CONNECTED",
          "connected": true,
          "source": "SIMULATED",
          "demo": true,
          "lastReadingAt": "2026-01-02T03:04:05Z",
          "message": "Simulated meter connected (DEMO DATA)"
        }
        """

        const val SUMMARY_JSON = """
        {
          "from": "2026-01-02T00:00:00Z",
          "to": "2026-01-02T12:00:00Z",
          "readingCount": 42,
          "energyImportedKwh": 8.5,
          "energyExportedKwh": 0.0,
          "averagePowerKw": 1.1,
          "peakPowerKw": 4.2,
          "minPowerKw": 0.1,
          "averageVoltage": 229.8,
          "minVoltage": 224.0,
          "maxVoltage": 232.0,
          "averageCurrent": 4.8,
          "averagePowerFactor": 0.94,
          "averageFrequency": 50.0,
          "source": "SIMULATED",
          "demo": true
        }
        """

        val HISTORY_JSON = """
        {
          "content": [$READING_JSON],
          "page": 0,
          "size": 50,
          "totalElements": 1,
          "totalPages": 1,
          "first": true,
          "last": true
        }
        """
    }
}
