package com.homeenergymonitor.meter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.homeenergymonitor.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulatedMeterAdapterTest {

    private static SimulatedMeterAdapter adapter(MutableClock clock, long seed) {
        return new SimulatedMeterAdapter("SIM-0001", 230.0, 50.0, clock, new Random(seed));
    }

    private static MutableClock clockAt(String instant) {
        return new MutableClock(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Test
    void readRequiresAConnection() {
        MutableClock clock = clockAt("2026-03-01T12:00:00Z");
        SimulatedMeterAdapter adapter = adapter(clock, 1L);

        assertThat(adapter.isConnected()).isFalse();
        assertThatThrownBy(adapter::read).isInstanceOf(IllegalStateException.class);

        adapter.start();
        assertThat(adapter.isConnected()).isTrue();
        assertThat(adapter.read()).isNotNull();

        adapter.stop();
        assertThat(adapter.isConnected()).isFalse();
    }

    @Test
    void readingsAreElectricallyConsistent() {
        MutableClock clock = clockAt("2026-03-01T19:00:00Z");
        SimulatedMeterAdapter adapter = adapter(clock, 42L);
        adapter.start();

        for (int i = 0; i < 500; i++) {
            MeterSample sample = adapter.read();
            double expectedPowerKw = sample.voltage() * sample.current() * sample.powerFactor() / 1000.0;

            assertThat(sample.activePowerKw())
                    .as("P must equal V * I * PF / 1000")
                    .isCloseTo(expectedPowerKw, within(0.02));
            assertThat(sample.voltage()).isBetween(218.0, 242.0);
            assertThat(sample.current()).isGreaterThanOrEqualTo(0.0);
            assertThat(sample.frequency()).isBetween(49.8, 50.2);
            assertThat(sample.powerFactor()).isBetween(0.85, 0.99);
            assertThat(sample.cumulativeImportKwh()).isGreaterThanOrEqualTo(0.0);

            clock.advance(Duration.ofSeconds(3));
        }
    }

    @Test
    void cumulativeEnergyUsesPowerTimesElapsedTime() {
        MutableClock clock = clockAt("2026-03-01T12:00:00Z");
        SimulatedMeterAdapter adapter = adapter(clock, 7L);
        adapter.start();

        MeterSample first = adapter.read();
        assertThat(first.cumulativeImportKwh()).isZero();

        clock.advance(Duration.ofSeconds(3));
        MeterSample second = adapter.read();
        double expectedIncrement = second.activePowerKw() * 3.0 / 3600.0;
        assertThat(second.cumulativeImportKwh())
                .isCloseTo(first.cumulativeImportKwh() + expectedIncrement, within(1e-5));

        clock.advance(Duration.ofHours(1));
        MeterSample third = adapter.read();
        double expectedHourIncrement = third.activePowerKw() * 3600.0 / 3600.0;
        assertThat(third.cumulativeImportKwh())
                .isCloseTo(second.cumulativeImportKwh() + expectedHourIncrement, within(1e-5));
    }

    @Test
    void cumulativeEnergyIsMonotonicAndNeverRandom() {
        MutableClock clock = clockAt("2026-03-01T08:00:00Z");
        SimulatedMeterAdapter adapter = adapter(clock, 123L);
        adapter.start();

        double previous = 0.0;
        for (int i = 0; i < 200; i++) {
            MeterSample sample = adapter.read();
            assertThat(sample.cumulativeImportKwh()).isGreaterThanOrEqualTo(previous);
            previous = sample.cumulativeImportKwh();
            clock.advance(Duration.ofSeconds(3));
        }
        assertThat(previous).isGreaterThan(0.0);
    }

    @Test
    void profileIsLowerOvernightThanInTheEvening() {
        MutableClock overnight = clockAt("2026-03-01T02:00:00Z");
        MutableClock evening = clockAt("2026-03-01T19:00:00Z");

        double overnightAverage = averagePower(adapter(overnight, 99L), overnight);
        double eveningAverage = averagePower(adapter(evening, 99L), evening);

        assertThat(overnightAverage).isLessThan(1.0);
        assertThat(eveningAverage).isGreaterThan(1.5);
        assertThat(eveningAverage).isGreaterThan(overnightAverage);
    }

    private static double averagePower(SimulatedMeterAdapter adapter, MutableClock clock) {
        adapter.start();
        double total = 0.0;
        int samples = 400;
        for (int i = 0; i < samples; i++) {
            total += adapter.read().activePowerKw();
            clock.advance(Duration.ofSeconds(3));
        }
        return total / samples;
    }

    @Test
    void statusIdentifiesSimulatedSource() {
        MutableClock clock = clockAt("2026-03-01T12:00:00Z");
        SimulatedMeterAdapter adapter = adapter(clock, 3L);
        adapter.start();
        MeterSample sample = adapter.read();

        MeterStatus status = adapter.status();
        assertThat(status.source()).isEqualTo("SIMULATED");
        assertThat(status.connectionState()).isEqualTo(MeterConnectionState.CONNECTED);
        assertThat(status.connected()).isTrue();
        assertThat(status.meterSerial()).isEqualTo("SIM-0001");
        assertThat(status.lastReadingAt()).isEqualTo(sample.timestamp());
    }
}
