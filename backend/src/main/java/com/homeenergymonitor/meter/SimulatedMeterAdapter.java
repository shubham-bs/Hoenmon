package com.homeenergymonitor.meter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simulated single-phase household meter.
 *
 * <p>Produces a plausible daily consumption profile (low overnight, morning
 * activity, daytime variation, evening peak, occasional appliance spikes) and keeps
 * cumulative import energy consistent by integrating power over the elapsed time.
 * All output is DEMO DATA: no real hardware is involved.</p>
 */
@Component
public class SimulatedMeterAdapter implements MeterAdapter {

    private final String meterSerial;
    private final double nominalVoltage;
    private final double nominalFrequency;
    private final Clock clock;
    private final Random random;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private volatile Instant lastReadingTimestamp;
    private double cumulativeImportKwh;
    private double cumulativeExportKwh;

    public SimulatedMeterAdapter(
            @Value("${app.simulation.meter-serial:SIM-0001}") String meterSerial,
            @Value("${app.simulation.nominal-voltage:230.0}") double nominalVoltage,
            @Value("${app.simulation.nominal-frequency:50.0}") double nominalFrequency,
            Clock clock,
            Random random) {
        this.meterSerial = meterSerial;
        this.nominalVoltage = nominalVoltage;
        this.nominalFrequency = nominalFrequency;
        this.clock = clock;
        this.random = random;
    }

    @PostConstruct
    void onStart() {
        start();
    }

    @PreDestroy
    void onStop() {
        stop();
    }

    @Override
    public String meterSerial() {
        return meterSerial;
    }

    @Override
    public void start() {
        connected.set(true);
    }

    @Override
    public void stop() {
        connected.set(false);
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public synchronized MeterSample read() {
        if (!connected.get()) {
            throw new IllegalStateException("Simulated meter adapter is not connected");
        }

        Instant now = clock.instant();
        double elapsedSeconds = 0.0;
        if (lastReadingTimestamp != null) {
            long elapsedMillis = Duration.between(lastReadingTimestamp, now).toMillis();
            elapsedSeconds = Math.max(0L, elapsedMillis) / 1000.0;
        }

        double targetPowerKw = simulateActivePower(now);
        double voltage = round(clamp(
                nominalVoltage + 2.0 - Math.min(6.0, targetPowerKw * 0.6)
                        + (random.nextDouble() - 0.5) * 2.0,
                nominalVoltage - 12.0, nominalVoltage + 8.0), 3);
        double powerFactor = round(clamp(
                0.90 + Math.min(0.09, targetPowerKw * 0.02)
                        + (random.nextDouble() - 0.5) * 0.02,
                0.85, 0.99), 4);
        double current = round((targetPowerKw * 1000.0) / (voltage * powerFactor), 4);
        double activePowerKw = round((voltage * current * powerFactor) / 1000.0, 4);
        double frequency = round(clamp(
                nominalFrequency + (random.nextDouble() - 0.5) * 0.1, 49.8, 50.2), 3);

        double incrementKwh = Math.max(0.0, activePowerKw) * elapsedSeconds / 3600.0;
        cumulativeImportKwh += incrementKwh;

        lastReadingTimestamp = now;
        return new MeterSample(
                meterSerial,
                now,
                activePowerKw,
                voltage,
                current,
                frequency,
                powerFactor,
                round(cumulativeImportKwh, 6),
                round(cumulativeExportKwh, 6));
    }

    @Override
    public MeterStatus status() {
        boolean isUp = connected.get();
        return new MeterStatus(
                meterSerial,
                isUp ? MeterConnectionState.CONNECTED : MeterConnectionState.DISCONNECTED,
                MeterSource.SIMULATED.name(),
                lastReadingTimestamp,
                isUp ? "Simulated meter connected (DEMO DATA)" : "Simulated meter disconnected");
    }

    private double simulateActivePower(Instant now) {
        ZonedDateTime zoned = now.atZone(clock.getZone());
        int hour = zoned.getHour();
        double base;
        if (hour < 6) {
            base = 0.25;
        } else if (hour < 9) {
            base = 1.2;
        } else if (hour < 17) {
            base = 0.7;
        } else if (hour < 22) {
            base = 2.4;
        } else {
            base = 0.5;
        }

        double power = base * (1.0 + (random.nextDouble() - 0.5) * 0.2);
        if (random.nextDouble() < 0.05) {
            power += 0.5 + random.nextDouble() * 2.5;
        }
        return clamp(power, 0.05, 12.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
