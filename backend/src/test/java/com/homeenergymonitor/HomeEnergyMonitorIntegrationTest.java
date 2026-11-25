package com.homeenergymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homeenergymonitor.persistence.MeterReadingRepository;
import com.homeenergymonitor.telemetry.TelemetryService;
import com.homeenergymonitor.telemetry.TelemetrySseBroadcaster;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * End-to-end verification against a real PostgreSQL instance: Flyway migrations,
 * JPA persistence, REST endpoints and the SSE stream start.
 */
@SpringBootTest
@ActiveProfiles("test")
class HomeEnergyMonitorIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private TelemetrySseBroadcaster broadcaster;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        meterReadingRepository.deleteAll();
    }

    @Test
    void flywayMigrationsCreateSchemaAndProvisionMeter() {
        Integer tables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = 'public' and table_name in ('smart_meter', 'meter_reading')",
                Integer.class);
        assertThat(tables).isEqualTo(2);

        Integer migrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(migrations).isGreaterThanOrEqualTo(1);

        Integer meters = jdbcTemplate.queryForObject("select count(*) from smart_meter", Integer.class);
        assertThat(meters).isGreaterThanOrEqualTo(1);
    }

    @Test
    void capturePersistsReadingAndCurrentEndpointReturnsIt() throws Exception {
        Optional<MeterReadingDto> captured = telemetryService.captureOnce();

        assertThat(captured).isPresent();
        assertThat(meterReadingRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/meter/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.meterId").value("SIM-0001"))
                .andExpect(jsonPath("$.activePowerKw").isNumber())
                .andExpect(jsonPath("$.cumulativeImportKwh").isNumber());
    }

    @Test
    void statusHistoryAndSummaryEndpointsRespond() throws Exception {
        telemetryService.captureOnce();
        telemetryService.captureOnce();

        mockMvc.perform(get("/api/v1/meter/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionState").value("CONNECTED"))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.lastReadingAt").exists());

        mockMvc.perform(get("/api/v1/meter/history").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10));

        mockMvc.perform(get("/api/v1/meter/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingCount").value(2))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true));
    }

    @Test
    void invalidHistoryRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/meter/history")
                        .param("from", "2026-03-02T00:00:00Z")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.demo").value(true));
    }

    @Test
    void sseStreamStartsAsynchronously() throws Exception {
        try {
            mockMvc.perform(get("/api/v1/telemetry/stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted());
        } finally {
            broadcaster.shutdown();
        }
    }
}
