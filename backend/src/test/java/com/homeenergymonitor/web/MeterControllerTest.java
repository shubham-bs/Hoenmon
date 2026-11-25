package com.homeenergymonitor.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homeenergymonitor.meter.MeterQueryService;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import com.homeenergymonitor.web.dto.MeterStatusDto;
import com.homeenergymonitor.web.dto.MeterSummaryDto;
import com.homeenergymonitor.web.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MeterControllerTest {

    private final MeterQueryService queries = mock(MeterQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MeterController(queries))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void currentReturnsReadingWithDemoFlags() throws Exception {
        when(queries.current()).thenReturn(Optional.of(reading()));

        mockMvc.perform(get("/api/v1/meter/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.meterId").value("SIM-0001"))
                .andExpect(jsonPath("$.activePowerKw").value(1.25))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void currentReturnsNoContentWhenNoReadingExists() throws Exception {
        when(queries.current()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/meter/current"))
                .andExpect(status().isNoContent());
    }

    @Test
    void statusExposesConnectionState() throws Exception {
        when(queries.status()).thenReturn(new MeterStatusDto("SIM-0001", "CONNECTED", true,
                "SIMULATED", true, Instant.parse("2026-03-01T10:00:00Z"), "Simulated meter connected"));

        mockMvc.perform(get("/api/v1/meter/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meterId").value("SIM-0001"))
                .andExpect(jsonPath("$.connectionState").value("CONNECTED"))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.lastReadingAt").exists());
    }

    @Test
    void historyReturnsPagedReadings() throws Exception {
        when(queries.history(any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageResponse<>(List.of(reading()), 0, 50, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/meter/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].meterId").value("SIM-0001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void summaryReturnsAggregates() throws Exception {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-02T00:00:00Z");
        when(queries.summary(any(), any())).thenReturn(new MeterSummaryDto(from, to, 2,
                0.5, 0.0, 1.2, 2.5, 0.5, 230.0, 228.0, 232.0, 5.2, 0.95, 50.0, "SIMULATED", true));

        mockMvc.perform(get("/api/v1/meter/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingCount").value(2))
                .andExpect(jsonPath("$.energyImportedKwh").value(0.5))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true));
    }

    @Test
    void invalidHistoryParametersReturnBadRequest() throws Exception {
        when(queries.history(any(), any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new IllegalArgumentException("'page' must be greater than or equal to 0"));

        mockMvc.perform(get("/api/v1/meter/history").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true));
    }

    private static MeterReadingDto reading() {
        return new MeterReadingDto(1L, "SIM-0001", Instant.parse("2026-03-01T10:00:00Z"),
                1.25, 230.1, 5.72, 50.0, 0.95, 12.5, 0.0, "SIMULATED", true);
    }
}
