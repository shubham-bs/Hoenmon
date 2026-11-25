package com.homeenergymonitor.web.dto;

import com.homeenergymonitor.persistence.MeterReading;
import java.time.Instant;

public record MeterReadingDto(
        Long id,
        String meterId,
        Instant timestamp,
        double activePowerKw,
        double voltage,
        double current,
        double frequency,
        double powerFactor,
        double cumulativeImportKwh,
        double cumulativeExportKwh,
        String source,
        boolean demo) {

    public static MeterReadingDto from(MeterReading reading, String meterSerial) {
        return new MeterReadingDto(
                reading.getId(),
                meterSerial,
                reading.getReadingTimestamp(),
                reading.getActivePowerKw().doubleValue(),
                reading.getVoltage().doubleValue(),
                reading.getCurrent().doubleValue(),
                reading.getFrequency().doubleValue(),
                reading.getPowerFactor().doubleValue(),
                reading.getCumulativeImportKwh().doubleValue(),
                reading.getCumulativeExportKwh().doubleValue(),
                reading.getSource(),
                true);
    }
}
