package com.homeenergymonitor.data.remote

import com.homeenergymonitor.data.remote.dto.MeterReadingDto
import com.homeenergymonitor.data.remote.dto.MeterStatusDto
import com.homeenergymonitor.data.remote.dto.MeterSummaryDto
import com.homeenergymonitor.data.remote.dto.PageResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MeterApi {

    @GET("api/v1/meter/current")
    suspend fun current(): Response<MeterReadingDto>

    @GET("api/v1/meter/status")
    suspend fun status(): MeterStatusDto

    @GET("api/v1/meter/summary")
    suspend fun summary(
        @Query("from") from: String,
        @Query("to") to: String
    ): MeterSummaryDto

    @GET("api/v1/meter/history")
    suspend fun history(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("order") order: String = "desc"
    ): PageResponseDto<MeterReadingDto>
}
