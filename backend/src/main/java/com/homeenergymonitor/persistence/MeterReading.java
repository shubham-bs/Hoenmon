package com.homeenergymonitor.persistence;

import com.homeenergymonitor.meter.MeterSample;
import com.homeenergymonitor.meter.MeterSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "meter_reading")
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private SmartMeter meter;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant readingTimestamp;

    @Column(name = "active_power_kw", nullable = false, precision = 10, scale = 4)
    private BigDecimal activePowerKw;

    @Column(name = "voltage", nullable = false, precision = 7, scale = 3)
    private BigDecimal voltage;

    @Column(name = "current", nullable = false, precision = 8, scale = 3)
    private BigDecimal current;

    @Column(name = "frequency", nullable = false, precision = 6, scale = 3)
    private BigDecimal frequency;

    @Column(name = "power_factor", nullable = false, precision = 5, scale = 4)
    private BigDecimal powerFactor;

    @Column(name = "cumulative_import_kwh", nullable = false, precision = 14, scale = 6)
    private BigDecimal cumulativeImportKwh;

    @Column(name = "cumulative_export_kwh", nullable = false, precision = 14, scale = 6)
    private BigDecimal cumulativeExportKwh;

    @Column(name = "source", nullable = false, length = 16)
    private String source;

    protected MeterReading() {
    }

    public static MeterReading from(MeterSample sample, SmartMeter meter) {
        MeterReading reading = new MeterReading();
        reading.meter = meter;
        reading.readingTimestamp = sample.timestamp();
        reading.activePowerKw = decimal(sample.activePowerKw(), 4);
        reading.voltage = decimal(sample.voltage(), 3);
        reading.current = decimal(sample.current(), 3);
        reading.frequency = decimal(sample.frequency(), 3);
        reading.powerFactor = decimal(sample.powerFactor(), 4);
        reading.cumulativeImportKwh = decimal(sample.cumulativeImportKwh(), 6);
        reading.cumulativeExportKwh = decimal(sample.cumulativeExportKwh(), 6);
        reading.source = MeterSource.SIMULATED.name();
        return reading;
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public SmartMeter getMeter() {
        return meter;
    }

    public Instant getReadingTimestamp() {
        return readingTimestamp;
    }

    public BigDecimal getActivePowerKw() {
        return activePowerKw;
    }

    public BigDecimal getVoltage() {
        return voltage;
    }

    public BigDecimal getCurrent() {
        return current;
    }

    public BigDecimal getFrequency() {
        return frequency;
    }

    public BigDecimal getPowerFactor() {
        return powerFactor;
    }

    public BigDecimal getCumulativeImportKwh() {
        return cumulativeImportKwh;
    }

    public BigDecimal getCumulativeExportKwh() {
        return cumulativeExportKwh;
    }

    public String getSource() {
        return source;
    }
}
