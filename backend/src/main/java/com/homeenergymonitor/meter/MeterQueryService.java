package com.homeenergymonitor.meter;

import com.homeenergymonitor.analytics.SummaryCalculator;
import com.homeenergymonitor.persistence.MeterReading;
import com.homeenergymonitor.persistence.MeterReadingRepository;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import com.homeenergymonitor.web.dto.MeterStatusDto;
import com.homeenergymonitor.web.dto.MeterSummaryDto;
import com.homeenergymonitor.web.dto.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeterQueryService {

    private static final int MAX_PAGE_SIZE = 500;

    private final MeterReadingRepository readings;
    private final MeterAdapter meterAdapter;
    private final SummaryCalculator summaryCalculator;
    private final Clock clock;

    public MeterQueryService(
            MeterReadingRepository readings,
            MeterAdapter meterAdapter,
            SummaryCalculator summaryCalculator,
            Clock clock) {
        this.readings = readings;
        this.meterAdapter = meterAdapter;
        this.summaryCalculator = summaryCalculator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<MeterReadingDto> current() {
        return readings.findTopByOrderByReadingTimestampDesc()
                .map(reading -> MeterReadingDto.from(reading, reading.getMeter().getSerialNumber()));
    }

    @Transactional(readOnly = true)
    public MeterStatusDto status() {
        MeterStatus adapterStatus = meterAdapter.status();
        Instant lastReadingAt = adapterStatus.lastReadingAt();
        if (lastReadingAt == null) {
            lastReadingAt = readings.findTopByOrderByReadingTimestampDesc()
                    .map(MeterReading::getReadingTimestamp)
                    .orElse(null);
        }
        return new MeterStatusDto(
                adapterStatus.meterSerial(),
                adapterStatus.connectionState().name(),
                adapterStatus.connected(),
                adapterStatus.source(),
                true,
                lastReadingAt,
                adapterStatus.message());
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterReadingDto> history(Instant from, Instant to, int page, int size, String order) {
        validateRange(from, to);
        if (page < 0) {
            throw new IllegalArgumentException("'page' must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("'size' must be between 1 and " + MAX_PAGE_SIZE);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "readingTimestamp"));

        Page<MeterReading> result;
        if (from != null && to != null) {
            result = readings.findByReadingTimestampBetween(from, to, pageable);
        } else if (from != null) {
            result = readings.findByReadingTimestampGreaterThanEqual(from, pageable);
        } else if (to != null) {
            result = readings.findByReadingTimestampLessThanEqual(to, pageable);
        } else {
            result = readings.findAll(pageable);
        }

        List<MeterReadingDto> content = result.getContent().stream()
                .map(reading -> MeterReadingDto.from(reading, reading.getMeter().getSerialNumber()))
                .toList();
        return PageResponse.of(content, result);
    }

    @Transactional(readOnly = true)
    public MeterSummaryDto summary(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : clock.instant();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(Duration.ofHours(24));
        validateRange(effectiveFrom, effectiveTo);

        List<MeterReading> rows = readings
                .findByReadingTimestampBetweenOrderByReadingTimestampAsc(effectiveFrom, effectiveTo);
        return summaryCalculator.calculate(rows, effectiveFrom, effectiveTo);
    }

    private static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }
}
