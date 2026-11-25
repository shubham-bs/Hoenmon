package com.homeenergymonitor.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartMeterRepository extends JpaRepository<SmartMeter, Long> {

    Optional<SmartMeter> findBySerialNumber(String serialNumber);
}
