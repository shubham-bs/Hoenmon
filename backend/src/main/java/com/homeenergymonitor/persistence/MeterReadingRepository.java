package com.homeenergymonitor.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    @EntityGraph(attributePaths = "meter")
    Optional<MeterReading> findTopByOrderByReadingTimestampDesc();

    @EntityGraph(attributePaths = "meter")
    Page<MeterReading> findByReadingTimestampGreaterThanEqual(Instant from, Pageable pageable);

    @EntityGraph(attributePaths = "meter")
    Page<MeterReading> findByReadingTimestampLessThanEqual(Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "meter")
    Page<MeterReading> findByReadingTimestampBetween(Instant from, Instant to, Pageable pageable);

    List<MeterReading> findByReadingTimestampBetweenOrderByReadingTimestampAsc(Instant from, Instant to);
}
