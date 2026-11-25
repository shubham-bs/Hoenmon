package com.homeenergymonitor.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.homeenergymonitor.BuildConfig
import com.homeenergymonitor.data.local.HomeEnergyDatabase
import com.homeenergymonitor.data.local.RoomReadingCache
import com.homeenergymonitor.data.remote.MeterApi
import com.homeenergymonitor.data.remote.SseTelemetryClient
import com.homeenergymonitor.data.remote.buildGson
import com.homeenergymonitor.data.repository.DefaultMeterRepository
import com.homeenergymonitor.data.repository.MeterRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {

    val baseUrl: String = normalizeBaseUrl(BuildConfig.API_BASE_URL)

    private val gson: Gson = buildGson()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val restClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val sseHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    private val api: MeterApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(restClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(MeterApi::class.java)

    private val database: HomeEnergyDatabase = Room.databaseBuilder(
        context,
        HomeEnergyDatabase::class.java,
        "home-energy-monitor.db"
    ).build()

    private val sseClient = SseTelemetryClient(sseHttpClient, baseUrl)

    val repository: MeterRepository = DefaultMeterRepository(
        api = api,
        sseClient = sseClient,
        cache = RoomReadingCache(database.readingDao()),
        gson = gson
    )

    private fun normalizeBaseUrl(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
