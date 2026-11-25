package com.homeenergymonitor.ui.dashboard

import com.google.common.truth.Truth.assertThat
import com.homeenergymonitor.MainDispatcherRule
import com.homeenergymonitor.data.repository.MeterRepository
import com.homeenergymonitor.domain.model.HistoryPage
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.MeterStatus
import com.homeenergymonitor.domain.model.MeterSummary
import com.homeenergymonitor.domain.model.TelemetryEvent
import com.homeenergymonitor.sampleHistoryPage
import com.homeenergymonitor.sampleReading
import com.homeenergymonitor.sampleStatus
import com.homeenergymonitor.sampleSummary
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsInitialDataThenAppliesLiveReadings() = runTest {
        val repository = FakeMeterRepository()
        val viewModel = DashboardViewModel(repository)

        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertThat(initial.isInitialLoading).isFalse()
        assertThat(initial.latestReading?.id).isEqualTo(repository.currentResult?.id)
        assertThat(initial.todaySummary).isNotNull()
        assertThat(initial.status).isNotNull()
        assertThat(initial.demoData).isTrue()

        repository.telemetryFlow.emit(TelemetryEvent.Connected(Instant.now()))
        repository.telemetryFlow.emit(
            TelemetryEvent.Reading(sampleReading(id = 2L, activePowerKw = 2.5))
        )
        advanceUntilIdle()

        val live = viewModel.uiState.value
        assertThat(live.connection).isEqualTo(LiveConnection.LIVE)
        assertThat(live.latestReading?.id).isEqualTo(2L)
        assertThat(live.latestReading?.activePowerKw).isEqualTo(2.5)
        assertThat(live.lastLiveUpdateAt).isNotNull()
    }

    @Test
    fun exposesReconnectingState() = runTest {
        val repository = FakeMeterRepository()
        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        repository.telemetryFlow.emit(
            TelemetryEvent.Reconnecting(attempt = 3, delayMillis = 4_000, reason = "boom")
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.connection).isEqualTo(LiveConnection.RECONNECTING)
        assertThat(state.reconnectAttempt).isEqualTo(3)
        assertThat(state.errorMessage).isEqualTo("boom")
    }

    @Test
    fun reportsBackendFailureAsFriendlyMessage() = runTest {
        val repository = FakeMeterRepository().apply {
            currentError = IOException("unreachable")
        }
        val viewModel = DashboardViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isInitialLoading).isFalse()
        assertThat(state.errorMessage).contains("backend")
    }

    @Test
    fun paginatesHistoryAndDoesNotReloadInitialPage() = runTest {
        val repository = FakeMeterRepository()
        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        viewModel.loadInitialHistory()
        advanceUntilIdle()

        assertThat(viewModel.historyState.value.items).hasSize(1)
        assertThat(viewModel.historyState.value.hasLoaded).isTrue()

        viewModel.loadHistory(refresh = false)
        advanceUntilIdle()

        assertThat(viewModel.historyState.value.items).hasSize(2)
        assertThat(viewModel.historyState.value.isLast).isTrue()

        viewModel.loadInitialHistory()
        advanceUntilIdle()

        assertThat(viewModel.historyState.value.items).hasSize(2)
    }

    private class FakeMeterRepository : MeterRepository {

        val telemetryFlow = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 32)
        override val cachedReadings: Flow<List<MeterReading>> =
            MutableStateFlow<List<MeterReading>>(emptyList())

        var currentResult: MeterReading? = sampleReading()
        var currentError: Throwable? = null
        var statusError: Throwable? = null
        var summaryError: Throwable? = null
        var historyError: Throwable? = null

        override fun telemetry(): Flow<TelemetryEvent> = telemetryFlow

        override suspend fun current(): MeterReading? {
            currentError?.let { throw it }
            return currentResult
        }

        override suspend fun status(): MeterStatus {
            statusError?.let { throw it }
            return sampleStatus()
        }

        override suspend fun summary(from: Instant, to: Instant): MeterSummary {
            summaryError?.let { throw it }
            return sampleSummary()
        }

        override suspend fun history(
            from: Instant,
            to: Instant,
            page: Int,
            size: Int
        ): HistoryPage {
            historyError?.let { throw it }
            return if (page == 0) {
                sampleHistoryPage(
                    page = 0,
                    last = false,
                    readings = listOf(sampleReading(id = 1L))
                )
            } else {
                sampleHistoryPage(
                    page = page,
                    last = true,
                    readings = listOf(sampleReading(id = 2L))
                )
            }
        }
    }
}
