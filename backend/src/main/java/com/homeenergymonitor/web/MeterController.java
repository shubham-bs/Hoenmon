package com.homeenergymonitor.web;

import com.homeenergymonitor.meter.MeterQueryService;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import com.homeenergymonitor.web.dto.MeterStatusDto;
import com.homeenergymonitor.web.dto.MeterSummaryDto;
import com.homeenergymonitor.web.dto.PageResponse;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meter")
public class MeterController {

    private final MeterQueryService meterQueryService;

    public MeterController(MeterQueryService meterQueryService) {
        this.meterQueryService = meterQueryService;
    }

    @GetMapping("/current")
    public ResponseEntity<MeterReadingDto> current() {
        return meterQueryService.current()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public PageResponse<MeterReadingDto> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String order) {
        return meterQueryService.history(from, to, page, size, order);
    }

    @GetMapping("/status")
    public MeterStatusDto status() {
        return meterQueryService.status();
    }

    @GetMapping("/summary")
    public MeterSummaryDto summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return meterQueryService.summary(from, to);
    }
}
