package com.homeenergymonitor.telemetry;

import com.homeenergymonitor.meter.MeterAdapter;
import com.homeenergymonitor.meter.MeterSample;
import com.homeenergymonitor.persistence.MeterReading;
import com.homeenergymonitor.persistence.ReadingStore;
import com.homeenergymonitor.persistence.SimulatedMeterService;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final MeterAdapter meterAdapter;
    private final SimulatedMeterService simulatedMeterService;
    private final ReadingStore readingStore;
    private final TelemetryValidator validator;
    private final TelemetrySseBroadcaster broadcaster;

    public TelemetryService(
            MeterAdapter meterAdapter,
            SimulatedMeterService simulatedMeterService,
            ReadingStore readingStore,
            TelemetryValidator validator,
            TelemetrySseBroadcaster broadcaster) {
        this.meterAdapter = meterAdapter;
        this.simulatedMeterService = simulatedMeterService;
        this.readingStore = readingStore;
        this.validator = validator;
        this.broadcaster = broadcaster;
    }

    /**
     * Captures one reading: obtain -> validate -> persist -> broadcast.
     *
     * <p>Public and side-effect scoped so tests and manual tooling can trigger a
     * deterministic capture. Returns empty when the reading is unavailable or invalid.</p>
     */
    public Optional<MeterReadingDto> captureOnce() {
        MeterSample sample;
        try {
            sample = meterAdapter.read();
        } catch (RuntimeException error) {
            log.warn("Meter adapter failed to produce a reading: {}", error.getMessage());
            return Optional.empty();
        }

        List<String> violations = validator.validate(sample);
        if (!violations.isEmpty()) {
            log.warn("Discarding invalid meter reading: {}", violations);
            return Optional.empty();
        }

        Long meterId = simulatedMeterService.getSimulatedMeterId();
        MeterReading saved = readingStore.save(meterId, sample);
        MeterReadingDto dto = MeterReadingDto.from(saved, sample.meterSerial());
        broadcaster.publish(dto);
        return Optional.of(dto);
    }
}
