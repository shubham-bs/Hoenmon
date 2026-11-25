package com.homeenergymonitor.data.remote

import com.google.common.truth.Truth.assertThat
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import com.homeenergymonitor.READING_JSON
import com.homeenergymonitor.data.remote.dto.MeterReadingDto
import com.homeenergymonitor.data.remote.dto.MeterStatusDto
import com.homeenergymonitor.data.remote.dto.MeterSummaryDto
import com.homeenergymonitor.data.remote.dto.PageResponseDto
import com.homeenergymonitor.data.remote.dto.toDomain
import java.time.Instant
import org.junit.Test

class DtoParsingTest {

    private val gson: Gson = buildGson()

    @Test
    fun parsesReadingAndIsoInstant() {
        val dto = gson.fromJson(READING_JSON, MeterReadingDto::class.java)

        assertThat(dto.id).isEqualTo(7L)
        assertThat(dto.meterId).isEqualTo("SIM-0001")
        assertThat(dto.timestamp).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"))
        assertThat(dto.powerFactor).isEqualTo(0.9512)
        assertThat(dto.demo).isTrue()

        val domain = dto.toDomain()
        assertThat(domain.activePowerKw).isEqualTo(1.234)
        assertThat(domain.timestamp).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"))
        assertThat(domain.demo).isTrue()
    }

    @Test
    fun parsesStatusWithNullLastReading() {
        val json = """
            {
              "meterId": "SIM-0001",
              "connectionState": "DISCONNECTED",
              "connected": false,
              "source": "SIMULATED",
              "demo": true,
              "lastReadingAt": null,
              "message": "Simulated meter disconnected"
            }
        """

        val dto = gson.fromJson(json, MeterStatusDto::class.java)

        assertThat(dto.connected).isFalse()
        assertThat(dto.lastReadingAt).isNull()
        assertThat(dto.toDomain().connectionState).isEqualTo("DISCONNECTED")
    }

    @Test
    fun parsesSummary() {
        val json = """
            {
              "from": "2026-01-02T00:00:00Z",
              "to": "2026-01-02T12:00:00Z",
              "readingCount": 42,
              "energyImportedKwh": 8.5,
              "energyExportedKwh": 0.0,
              "averagePowerKw": 1.1,
              "peakPowerKw": 4.2,
              "minPowerKw": 0.1,
              "averageVoltage": 229.8,
              "minVoltage": 224.0,
              "maxVoltage": 232.0,
              "averageCurrent": 4.8,
              "averagePowerFactor": 0.94,
              "averageFrequency": 50.0,
              "source": "SIMULATED",
              "demo": true
            }
        """

        val dto = gson.fromJson(json, MeterSummaryDto::class.java)

        assertThat(dto.readingCount).isEqualTo(42L)
        assertThat(dto.peakPowerKw).isEqualTo(4.2)
        assertThat(dto.toDomain().averageVoltage).isEqualTo(229.8)
    }

    @Test
    fun parsesPageResponseAndMapsToDomain() {
        val json = """
            {
              "content": [$READING_JSON],
              "page": 0,
              "size": 50,
              "totalElements": 1,
              "totalPages": 1,
              "first": true,
              "last": true
            }
        """
        val type = object : TypeToken<PageResponseDto<MeterReadingDto>>() {}.type
        val page: PageResponseDto<MeterReadingDto> = gson.fromJson(json, type)

        assertThat(page.content).hasSize(1)
        assertThat(page.totalElements).isEqualTo(1L)

        val domain = page.toDomain()
        assertThat(domain.content.first().id).isEqualTo(7L)
        assertThat(domain.last).isTrue()
    }
}
