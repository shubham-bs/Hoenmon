package com.homeenergymonitor.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.homeenergymonitor.meter.MeterSample;
import com.homeenergymonitor.persistence.MeterReading;
import com.homeenergymonitor.persistence.SmartMeter;
import com.homeenergymonitor.web.dto.MeterSummaryDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryCalculatorTest {

    private final SummaryCalculator calculator = new SummaryCalculator();
    private final SmartMeter meter = new SmartMeter("SIM-0001", "Home", "Demo", "SIM-1", "SINGLE_PHASE", true);

    @Test
    void emptyReadingsProduceZeroSummary() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-02T00:00:00Z");

        MeterSummaryDto summary = calculator.calculate(List.of(), from, to);

        assertThat(summary.readingCount()).isZero();
        assertThat(summary.energyImportedKwh()).isZero();
        assertThat(summary.averagePowerKw()).isZero();
        assertThat(summary.source()).isEqualTo("SIMULATED");
        assertThat(summary.demo()).isTrue();
    }

    @Test
    void aggregatesReadingsAndDerivesEnergyFromRegisters() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-02T00:00:00Z");
        List<MeterReading> readings = List.of(
                reading("2026-03-01T10:00:00Z", 1.0, 230.0, 4.5, 0.95, 50.0, 100.0),
                reading("2026-03-01T10:00:03Z", 2.0, 232.0, 9.0, 0.96, 50.1, 100.001),
                reading("2026-03-01T10:00:06Z", 3.0, 228.0, 13.5, 0.97, 49.9, 100.003));

        MeterSummaryDto summary = calculator.calculate(readings, from, to);

        assertThat(summary.readingCount()).isEqualTo(3);
        assertThat(summary.energyImportedKwh()).isCloseTo(0.003, within(1e-9));
        assertThat(summary.averagePowerKw()).isCloseTo(2.0, within(1e-9));
        assertThat(summary.peakPowerKw()).isCloseTo(3.0, within(1e-9));
        assertThat(summary.minPowerKw()).isCloseTo(1.0, within(1e-9));
        assertThat(summary.averageVoltage()).isCloseTo(230.0, within(1e-9));
        assertThat(summary.minVoltage()).isCloseTo(228.0, within(1e-9));
        assertThat(summary.maxVoltage()).isCloseTo(232.0, within(1e-9));
        assertThat(summary.averageCurrent()).isCloseTo(9.0, within(1e-9));
        assertThat(summary.averagePowerFactor()).isCloseTo(0.96, within(1e-9));
        assertThat(summary.averageFrequency()).isCloseTo(50.0, within(1e-9));
        assertThat(summary.source()).isEqualTo("SIMULATED");
        assertThat(summary.demo()).isTrue();
    }

    private MeterReading reading(String timestamp, double powerKw, double voltage, double current,
            double powerFactor, double frequency, double importKwh) {
        MeterSample sample = new MeterSample("SIM-0001", Instant.parse(timestamp), powerKw, voltage,
                current, frequency, powerFactor, importKwh, 0.0);
        return MeterReading.from(sample, meter);
    }
}
