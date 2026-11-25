package com.homeenergymonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.homeenergymonitor.data.repository.MeterRepository
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.domain.model.TelemetryEvent
import com.homeenergymonitor.domain.util.startOfToday
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val HISTORY_PAGE_SIZE = 50

class DashboardViewModel(
    private val repository: MeterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val cachedReadings: StateFlow<List<MeterReading>> = repository.cachedReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private var readingsSinceRefresh = 0
    private var backgroundRefresh: Job? = null

    init {
        observeTelemetry()
        refreshAll()
    }

    fun loadInitialHistory() {
        val history = _historyState.value
        if (!history.hasLoaded && !history.isLoading) {
            loadHistory(refresh = true)
        }
    }

    fun loadHistory(refresh: Boolean = false) {
        val history = _historyState.value
        if (history.isLoading) return
        val nextPage = if (refresh) 0 else history.page + 1

        viewModelScope.launch {
            _historyState.update {
                it.copy(isLoading = true, errorMessage = null)
            }
            runCatching {
                repository.history(
                    from = startOfToday(),
                    to = Instant.now(),
                    page = nextPage,
                    size = HISTORY_PAGE_SIZE
                )
            }.onSuccess { page ->
                _historyState.update {
                    it.copy(
                        items = if (refresh) page.content else it.items + page.content,
                        page = page.page,
                        isLast = page.last,
                        hasLoaded = true,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _historyState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = true,
                        errorMessage = describe(error)
                    )
                }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            var lastError: String? = null

            runCatching { repository.summary(startOfToday(), Instant.now()) }
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(todaySummary = summary, demoData = summary.demo)
                    }
                }
                .onFailure { lastError = describe(it) }

            runCatching { repository.status() }
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(status = status, demoData = status.demo || it.demoData)
                    }
                }
                .onFailure { if (lastError == null) lastError = describe(it) }

            runCatching { repository.current() }
                .onSuccess { reading ->
                    if (reading != null) {
                        _uiState.update {
                            it.copy(
                                latestReading = reading,
                                lastLiveUpdateAt = Instant.now(),
                                demoData = reading.demo
                            )
                        }
                    }
                }
                .onFailure { if (lastError == null) lastError = describe(it) }

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = lastError
                )
            }
        }
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            repository.telemetry().collect { event ->
                when (event) {
                    TelemetryEvent.Connecting -> _uiState.update {
                        it.copy(connection = LiveConnection.CONNECTING)
                    }

                    is TelemetryEvent.Connected -> {
                        _uiState.update {
                            it.copy(
                                connection = LiveConnection.LIVE,
                                reconnectAttempt = 0,
                                errorMessage = null,
                                isInitialLoading = false
                            )
                        }
                        refreshInBackground()
                    }

                    is TelemetryEvent.Reading -> {
                        readingsSinceRefresh++
                        _uiState.update {
                            it.copy(
                                latestReading = event.reading,
                                lastLiveUpdateAt = Instant.now(),
                                connection = LiveConnection.LIVE,
                                demoData = event.reading.demo,
                                errorMessage = null
                            )
                        }
                        if (readingsSinceRefresh >= 10) {
                            readingsSinceRefresh = 0
                            refreshInBackground()
                        }
                    }

                    is TelemetryEvent.Reconnecting -> _uiState.update {
                        it.copy(
                            connection = LiveConnection.RECONNECTING,
                            reconnectAttempt = event.attempt,
                            errorMessage = event.reason
                        )
                    }
                }
            }
        }
    }

    private fun refreshInBackground() {
        backgroundRefresh?.cancel()
        backgroundRefresh = viewModelScope.launch {
            runCatching { repository.summary(startOfToday(), Instant.now()) }
                .onSuccess { summary ->
                    _uiState.update { it.copy(todaySummary = summary) }
                }
            runCatching { repository.status() }
                .onSuccess { status ->
                    _uiState.update { it.copy(status = status) }
                }
        }
    }

    private fun describe(error: Throwable): String = when (error) {
        is IOException -> "Cannot reach the backend. Is it running on port 8080?"
        is HttpException -> "Backend returned HTTP ${error.code()}."
        else -> error.message ?: "Unexpected error while loading data."
    }

    companion object {
        fun factory(repository: MeterRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DashboardViewModel(repository) }
            }
    }
}
