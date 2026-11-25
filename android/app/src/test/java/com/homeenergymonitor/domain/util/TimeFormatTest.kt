package com.homeenergymonitor.domain.util

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class TimeFormatTest {

    private val now: Instant = Instant.parse("2026-01-02T12:00:00Z")

    @Test
    fun relativeTimeBuckets() {
        assertThat(formatRelativeTime(now, now)).isEqualTo("just now")
        assertThat(formatRelativeTime(now.minusSeconds(12), now)).isEqualTo("12s ago")
        assertThat(formatRelativeTime(now.minusSeconds(300), now)).isEqualTo("5m ago")
        assertThat(formatRelativeTime(now.minusSeconds(3 * 3600), now)).isEqualTo("3h ago")
        assertThat(formatRelativeTime(now.minusSeconds(2 * 86400), now)).isEqualTo("2d ago")
    }

    @Test
    fun staleDetection() {
        assertThat(isStale(null, now)).isTrue()
        assertThat(isStale(now.minusSeconds(5), now)).isFalse()
        assertThat(isStale(now.minusSeconds(11), now)).isTrue()
        assertThat(isStale(now.minusSeconds(30), now, thresholdSeconds = 60)).isFalse()
    }

    @Test
    fun formatsClockTimeInZone() {
        val instant = Instant.parse("2026-01-02T03:04:05Z")
        assertThat(formatClockTime(instant, ZoneId.of("UTC"))).isEqualTo("03:04:05")
    }

    @Test
    fun startOfTodayUsesProvidedInstantAndZone() {
        val start = startOfToday(now, ZoneId.of("UTC"))
        assertThat(start).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"))
    }
}
