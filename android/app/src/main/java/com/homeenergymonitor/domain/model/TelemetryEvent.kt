package com.homeenergymonitor.domain.model

import java.time.Instant

sealed interface TelemetryEvent {
    data object Connecting : TelemetryEvent
    data class Connected(val at: Instant) : TelemetryEvent
    data class Reading(val reading: MeterReading) : TelemetryEvent
    data class Reconnecting(
        val attempt: Int,
        val delayMillis: Long,
        val reason: String?
    ) : TelemetryEvent
}
