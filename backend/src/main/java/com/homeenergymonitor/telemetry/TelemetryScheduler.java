package com.homeenergymonitor.telemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives simulated telemetry. Disabled in tests via
 * {@code app.telemetry.scheduling-enabled=false} so captures are explicit.
 */
@Component
@ConditionalOnProperty(name = "app.telemetry.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryScheduler {

    private final TelemetryService telemetryService;
    private final TelemetrySseBroadcaster broadcaster;

    public TelemetryScheduler(TelemetryService telemetryService, TelemetrySseBroadcaster broadcaster) {
        this.telemetryService = telemetryService;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedRateString = "${app.telemetry.interval-ms:3000}")
    public void captureReading() {
        telemetryService.captureOnce();
    }

    @Scheduled(
            fixedRateString = "${app.telemetry.heartbeat-ms:15000}",
            initialDelayString = "${app.telemetry.heartbeat-ms:15000}")
    public void heartbeat() {
        broadcaster.heartbeat();
    }
}
