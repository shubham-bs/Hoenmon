package com.homeenergymonitor.meter;

import java.time.Instant;

/**
 * A single electrical measurement produced by a {@link MeterAdapter}.
 *
 * <p>Values are plain numbers (not rounded for storage). Cumulative registers are
 * the meter's running totals in kWh, not per-interval values.</p>
 */
public record MeterSample(
        String meterSerial,
        Instant timestamp,
        double activePowerKw,
        double voltage,
        double current,
        double frequency,
        double powerFactor,
        double cumulativeImportKwh,
        double cumulativeExportKwh) {
}
