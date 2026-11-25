package com.homeenergymonitor.persistence;

import com.homeenergymonitor.meter.MeterSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a validated sample in its own transaction so it is committed before it
 * is broadcast to SSE subscribers.
 */
@Service
public class ReadingStore {

    private final MeterReadingRepository readings;
    private final SmartMeterRepository meters;

    public ReadingStore(MeterReadingRepository readings, SmartMeterRepository meters) {
        this.readings = readings;
        this.meters = meters;
    }

    @Transactional
    public MeterReading save(Long meterId, MeterSample sample) {
        SmartMeter meterReference = meters.getReferenceById(meterId);
        return readings.save(MeterReading.from(sample, meterReference));
    }
}
