package com.homeenergymonitor.domain.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val STALE_AFTER_SECONDS: Long = 10L

fun isStale(
    lastUpdate: Instant?,
    now: Instant,
    thresholdSeconds: Long = STALE_AFTER_SECONDS
): Boolean {
    if (lastUpdate == null) return true
    return Duration.between(lastUpdate, now).seconds > thresholdSeconds
}

fun formatRelativeTime(instant: Instant, now: Instant): String {
    val seconds = Duration.between(instant, now).seconds.coerceAtLeast(0)
    return when {
        seconds < 5 -> "just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

fun formatClockTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    CLOCK_FORMAT.withZone(zone).format(instant)

fun formatDateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_TIME_FORMAT.withZone(zone).format(instant)

fun startOfToday(
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault()
): Instant = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

private val CLOCK_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

private val DATE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM, HH:mm:ss", Locale.US)
