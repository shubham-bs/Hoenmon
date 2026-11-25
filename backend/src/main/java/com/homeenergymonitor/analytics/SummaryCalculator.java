package com.homeenergymonitor.analytics;

import com.homeenergymonitor.meter.MeterSource;
import com.homeenergymonitor.persistence.MeterReading;
import com.homeenergymonitor.web.dto.MeterSummaryDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Aggregates persisted readings into a consumption summary. Energy is derived from
 * the difference between the cumulative meter registers, never from random values.
 */
@Component
public class SummaryCalculator {

    public MeterSummaryDto calculate(List<MeterReading> readings, Instant from, Instant to) {
        if (readings == null || readings.isEmpty()) {
            return new MeterSummaryDto(from, to, 0,
                    0, 0,
                    0, 0, 0,
                    0, 0, 0,
                    0, 0, 0,
                    MeterSource.SIMULATED.name(), true);
        }

        DoubleSummaryStatistics power = readings.stream()
                .mapToDouble(r -> r.getActivePowerKw().doubleValue()).summaryStatistics();
        DoubleSummaryStatistics voltage = readings.stream()
                .mapToDouble(r -> r.getVoltage().doubleValue()).summaryStatistics();
        DoubleSummaryStatistics current = readings.stream()
                .mapToDouble(r -> r.getCurrent().doubleValue()).summaryStatistics();
        DoubleSummaryStatistics powerFactor = readings.stream()
                .mapToDouble(r -> r.getPowerFactor().doubleValue()).summaryStatistics();
        DoubleSummaryStatistics frequency = readings.stream()
                .mapToDouble(r -> r.getFrequency().doubleValue()).summaryStatistics();

        BigDecimal minImport = readings.stream().map(MeterReading::getCumulativeImportKwh)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxImport = readings.stream().map(MeterReading::getCumulativeImportKwh)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal minExport = readings.stream().map(MeterReading::getCumulativeExportKwh)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxExport = readings.stream().map(MeterReading::getCumulativeExportKwh)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        double importedKwh = maxImport.subtract(minImport).max(BigDecimal.ZERO).doubleValue();
        double exportedKwh = maxExport.subtract(minExport).max(BigDecimal.ZERO).doubleValue();

        return new MeterSummaryDto(
                from,
                to,
                readings.size(),
                round(importedKwh, 3),
                round(exportedKwh, 3),
                round(power.getAverage(), 4),
                round(power.getMax(), 4),
                round(power.getMin(), 4),
                round(voltage.getAverage(), 3),
                round(voltage.getMin(), 3),
                round(voltage.getMax(), 3),
                round(current.getAverage(), 3),
                round(powerFactor.getAverage(), 4),
                round(frequency.getAverage(), 3),
                MeterSource.SIMULATED.name(),
                true);
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
