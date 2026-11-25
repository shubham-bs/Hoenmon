package com.homeenergymonitor.web.dto;

import java.time.Instant;

public record MeterSummaryDto(
        Instant from,
        Instant to,
        long readingCount,
        double energyImportedKwh,
        double energyExportedKwh,
        double averagePowerKw,
        double peakPowerKw,
        double minPowerKw,
        double averageVoltage,
        double minVoltage,
        double maxVoltage,
        double averageCurrent,
        double averagePowerFactor,
        double averageFrequency,
        String source,
        boolean demo) {
}
