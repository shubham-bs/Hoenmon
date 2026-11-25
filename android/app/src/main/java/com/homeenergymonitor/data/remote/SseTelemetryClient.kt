package com.homeenergymonitor.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

sealed interface SseSignal {
    data object Opened : SseSignal
    data class Event(val type: String, val data: String, val id: String?) : SseSignal
    data class Failure(val message: String) : SseSignal
    data object Closed : SseSignal
}

class SseTelemetryClient(
    private val client: OkHttpClient,
    baseUrl: String
) {
    private val streamUrl: String =
        baseUrl.trimEnd('/') + "/api/v1/telemetry/stream"

    fun streamOnce(): Flow<SseSignal> = callbackFlow {
        val request = Request.Builder()
            .url(streamUrl)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(SseSignal.Opened)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                trySend(SseSignal.Event(type ?: "message", data, id))
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SseSignal.Closed)
                close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val message = t?.message
                    ?: response?.let { "HTTP ${it.code}" }
                    ?: "Stream disconnected"
                trySend(SseSignal.Failure(message))
                close()
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }
}
