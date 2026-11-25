package com.homeenergymonitor.web.dto;

import java.time.Instant;

public record MeterStatusDto(
        String meterId,
        String connectionState,
        boolean connected,
        String source,
        boolean demo,
        Instant lastReadingAt,
        String message) {
}
