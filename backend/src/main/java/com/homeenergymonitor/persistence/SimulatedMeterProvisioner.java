package com.homeenergymonitor.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Provisions the simulated meter on startup so status/current endpoints have meter
 * metadata before the first scheduled reading.
 */
@Component
public class SimulatedMeterProvisioner implements ApplicationRunner {

    private final SimulatedMeterService simulatedMeterService;

    public SimulatedMeterProvisioner(SimulatedMeterService simulatedMeterService) {
        this.simulatedMeterService = simulatedMeterService;
    }

    @Override
    public void run(ApplicationArguments args) {
        simulatedMeterService.getSimulatedMeterId();
    }
}
