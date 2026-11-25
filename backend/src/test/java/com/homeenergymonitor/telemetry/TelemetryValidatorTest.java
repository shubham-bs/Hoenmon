package com.homeenergymonitor.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homeenergymonitor.meter.MeterSample;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TelemetryValidatorTest {

    private final TelemetryValidator validator = new TelemetryValidator();

    private static MeterSample sample(double powerKw, double voltage, double current,
            double frequency, double powerFactor, double importKwh, double exportKwh) {
        return new MeterSample("SIM-0001", Instant.parse("2026-03-01T12:00:00Z"),
                powerKw, voltage, current, frequency, powerFactor, importKwh, exportKwh);
    }

    @Test
    void acceptsConsistentSample() {
        MeterSample valid = sample(1.15, 230.0, 5.26, 50.0, 0.95, 10.0, 0.0);
        assertThat(validator.validate(valid)).isEmpty();
    }

    @Test
    void rejectsNullSample() {
        assertThat(validator.validate(null)).containsExactly("sample is null");
    }

    @Test
    void rejectsOutOfRangeVoltage() {
        MeterSample invalid = sample(1.15, 0.0, 5.26, 50.0, 0.95, 10.0, 0.0);
        assertThat(validator.validate(invalid)).contains("voltage out of range");
    }

    @Test
    void rejectsOutOfRangePowerFactor() {
        MeterSample invalid = sample(1.15, 230.0, 5.26, 50.0, 1.5, 10.0, 0.0);
        assertThat(validator.validate(invalid)).contains("power factor out of range");
    }

    @Test
    void rejectsNegativeCumulativeEnergy() {
        MeterSample invalid = sample(1.15, 230.0, 5.26, 50.0, 0.95, -1.0, 0.0);
        assertThat(validator.validate(invalid))
                .contains("cumulative import must be finite and non-negative");
    }

    @Test
    void rejectsPowerInconsistentWithOtherMeasurements() {
        MeterSample invalid = sample(9.0, 230.0, 5.26, 50.0, 0.95, 10.0, 0.0);
        assertThat(validator.validate(invalid))
                .contains("active power is inconsistent with voltage, current and power factor");
    }

    @Test
    void assertValidThrowsForInvalidSample() {
        assertThatThrownBy(() -> validator.assertValid(sample(9.0, 230.0, 5.26, 50.0, 0.95, 10.0, 0.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
