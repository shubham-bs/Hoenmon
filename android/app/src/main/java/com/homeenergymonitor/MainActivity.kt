package com.homeenergymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homeenergymonitor.ui.HomeEnergyMonitorRoot
import com.homeenergymonitor.ui.dashboard.DashboardViewModel
import com.homeenergymonitor.ui.theme.HomeEnergyMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as HomeEnergyMonitorApp).container

        setContent {
            HomeEnergyMonitorTheme {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.factory(container.repository)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val historyState by viewModel.historyState.collectAsStateWithLifecycle()
                val cachedReadings by viewModel.cachedReadings.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeEnergyMonitorRoot(
                        uiState = uiState,
                        historyState = historyState,
                        cachedReadings = cachedReadings,
                        onRefresh = viewModel::refreshAll,
                        onLoadInitialHistory = viewModel::loadInitialHistory,
                        onRefreshHistory = { viewModel.loadHistory(refresh = true) },
                        onLoadMoreHistory = { viewModel.loadHistory(refresh = false) }
                    )
                }
            }
        }
    }
}
