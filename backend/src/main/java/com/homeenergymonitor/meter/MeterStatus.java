package com.homeenergymonitor.meter;

import java.time.Instant;

public record MeterStatus(
        String meterSerial,
        MeterConnectionState connectionState,
        String source,
        Instant lastReadingAt,
        String message) {

    public boolean connected() {
        return connectionState == MeterConnectionState.CONNECTED;
    }
}
