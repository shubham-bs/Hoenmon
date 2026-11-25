package com.homeenergymonitor.data.remote

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class SseTelemetryClientTest {

    private val compactReading = """
        {"id":7,"meterId":"SIM-0001","timestamp":"2026-01-02T03:04:05Z","activePowerKw":1.234,"voltage":230.123,"current":5.4321,"frequency":50.012,"powerFactor":0.9512,"cumulativeImportKwh":12.345678,"cumulativeExportKwh":0.0,"source":"SIMULATED","demo":true}
    """.trimIndent()

    @Test
    fun streamsConnectedAndReadingEvents() = runBlocking {
        val server = MockWebServer()
        val body = buildString {
            append("event: connected\n")
            append("data: {\"type\":\"connected\"}\n\n")
            append("event: reading\n")
            append("id: 7\n")
            append("data: $compactReading\n\n")
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body)
        )
        server.start()

        try {
            val client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.SECONDS)
                .build()
            val sseClient = SseTelemetryClient(client, server.url("/").toString())

            val events = withTimeout(5_000) {
                sseClient.streamOnce()
                    .filterIsInstance<SseSignal.Event>()
                    .take(2)
                    .toList()
            }

            assertThat(events).hasSize(2)
            assertThat(events[0].type).isEqualTo("connected")
            assertThat(events[1].type).isEqualTo("reading")
            assertThat(events[1].id).isEqualTo("7")
            assertThat(events[1].data).contains("\"activePowerKw\":1.234")
        } finally {
            server.shutdown()
        }
    }
}
