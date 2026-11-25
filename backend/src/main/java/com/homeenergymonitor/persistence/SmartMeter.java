package com.homeenergymonitor.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "smart_meter")
public class SmartMeter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", nullable = false, unique = true, length = 64)
    private String serialNumber;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "manufacturer", length = 64)
    private String manufacturer;

    @Column(name = "model", length = 64)
    private String model;

    @Column(name = "phase_type", nullable = false, length = 16)
    private String phaseType;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected SmartMeter() {
    }

    public SmartMeter(String serialNumber, String name, String manufacturer, String model,
            String phaseType, boolean active) {
        this.serialNumber = serialNumber;
        this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
        this.phaseType = phaseType;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
