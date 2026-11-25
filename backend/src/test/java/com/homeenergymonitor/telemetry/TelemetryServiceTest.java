package com.homeenergymonitor.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.homeenergymonitor.meter.MeterAdapter;
import com.homeenergymonitor.meter.MeterSample;
import com.homeenergymonitor.persistence.MeterReading;
import com.homeenergymonitor.persistence.ReadingStore;
import com.homeenergymonitor.persistence.SimulatedMeterService;
import com.homeenergymonitor.persistence.SmartMeter;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private MeterAdapter meterAdapter;

    @Mock
    private SimulatedMeterService simulatedMeterService;

    @Mock
    private ReadingStore readingStore;

    @Mock
    private TelemetrySseBroadcaster broadcaster;

    private final TelemetryValidator validator = new TelemetryValidator();
    private TelemetryService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryService(meterAdapter, simulatedMeterService, readingStore, validator, broadcaster);
    }

    @Test
    void persistsAndBroadcastsAValidReading() {
        MeterSample sample = validSample();
        MeterReading entity = MeterReading.from(sample, meter());
        when(meterAdapter.read()).thenReturn(sample);
        when(simulatedMeterService.getSimulatedMeterId()).thenReturn(1L);
        when(readingStore.save(1L, sample)).thenReturn(entity);

        Optional<MeterReadingDto> result = service.captureOnce();

        assertThat(result).isPresent();
        assertThat(result.get().source()).isEqualTo("SIMULATED");
        assertThat(result.get().demo()).isTrue();
        assertThat(result.get().meterId()).isEqualTo("SIM-0001");
        verify(readingStore).save(1L, sample);
        verify(broadcaster).publish(any(MeterReadingDto.class));
    }

    @Test
    void doesNotPersistOrBroadcastAnInvalidReading() {
        MeterSample invalid = new MeterSample("SIM-0001", Instant.parse("2026-03-01T12:00:00Z"),
                9.0, 230.0, 5.26, 50.0, 0.95, 10.0, 0.0);
        when(meterAdapter.read()).thenReturn(invalid);

        Optional<MeterReadingDto> result = service.captureOnce();

        assertThat(result).isEmpty();
        verifyNoInteractions(readingStore, broadcaster);
    }

    @Test
    void swallowsAdapterFailures() {
        when(meterAdapter.read()).thenThrow(new IllegalStateException("not connected"));

        Optional<MeterReadingDto> result = service.captureOnce();

        assertThat(result).isEmpty();
        verifyNoInteractions(readingStore, broadcaster);
    }

    private static MeterSample validSample() {
        return new MeterSample("SIM-0001", Instant.parse("2026-03-01T12:00:00Z"),
                1.15, 230.0, 5.26, 50.0, 0.95, 10.0, 0.0);
    }

    private static SmartMeter meter() {
        return new SmartMeter("SIM-0001", "Home", "Demo", "SIM-1", "SINGLE_PHASE", true);
    }
}
