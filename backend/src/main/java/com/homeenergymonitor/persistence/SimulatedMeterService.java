package com.homeenergymonitor.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the single simulated meter row exists and exposes its id.
 */
@Service
public class SimulatedMeterService {

    static final String SIMULATED_MANUFACTURER = "SIMULATED";
    static final String SIMULATED_MODEL = "DEMO-SINGLE-PHASE";
    static final String SINGLE_PHASE = "SINGLE_PHASE";

    private final SmartMeterRepository repository;
    private final String serialNumber;
    private volatile Long cachedId;

    public SimulatedMeterService(
            SmartMeterRepository repository,
            @Value("${app.simulation.meter-serial:SIM-0001}") String serialNumber) {
        this.repository = repository;
        this.serialNumber = serialNumber;
    }

    @Transactional
    public synchronized Long getSimulatedMeterId() {
        if (cachedId != null) {
            return cachedId;
        }
        SmartMeter meter = repository.findBySerialNumber(serialNumber)
                .orElseGet(() -> repository.save(new SmartMeter(
                        serialNumber,
                        "Simulated Household Meter",
                        SIMULATED_MANUFACTURER,
                        SIMULATED_MODEL,
                        SINGLE_PHASE,
                        true)));
        cachedId = meter.getId();
        return cachedId;
    }
}
