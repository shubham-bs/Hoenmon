package com.homeenergymonitor.telemetry;

import com.homeenergymonitor.meter.MeterSample;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TelemetryValidator {

    private static final double POWER_TOLERANCE_KW = 0.02;

    public List<String> validate(MeterSample sample) {
        List<String> errors = new ArrayList<>();
        if (sample == null) {
            errors.add("sample is null");
            return errors;
        }
        if (sample.timestamp() == null) {
            errors.add("timestamp is required");
        }
        if (sample.meterSerial() == null || sample.meterSerial().isBlank()) {
            errors.add("meter serial is required");
        }
        if (!finite(sample.activePowerKw())) {
            errors.add("active power is not finite");
        }
        if (!finite(sample.voltage()) || sample.voltage() <= 0 || sample.voltage() > 400) {
            errors.add("voltage out of range");
        }
        if (!finite(sample.current()) || sample.current() < 0 || sample.current() > 200) {
            errors.add("current out of range");
        }
        if (!finite(sample.frequency()) || sample.frequency() < 45 || sample.frequency() > 65) {
            errors.add("frequency out of range");
        }
        if (!finite(sample.powerFactor()) || sample.powerFactor() < -1 || sample.powerFactor() > 1) {
            errors.add("power factor out of range");
        }
        if (!finite(sample.cumulativeImportKwh()) || sample.cumulativeImportKwh() < 0) {
            errors.add("cumulative import must be finite and non-negative");
        }
        if (!finite(sample.cumulativeExportKwh()) || sample.cumulativeExportKwh() < 0) {
            errors.add("cumulative export must be finite and non-negative");
        }

        if (errors.isEmpty()) {
            double expectedPowerKw = sample.voltage() * sample.current() * sample.powerFactor() / 1000.0;
            double tolerance = Math.max(POWER_TOLERANCE_KW, Math.abs(expectedPowerKw) * 0.05);
            if (Math.abs(expectedPowerKw - sample.activePowerKw()) > tolerance) {
                errors.add("active power is inconsistent with voltage, current and power factor");
            }
        }
        return errors;
    }

    public void assertValid(MeterSample sample) {
        List<String> errors = validate(sample);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid meter sample: " + errors);
        }
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }
}
