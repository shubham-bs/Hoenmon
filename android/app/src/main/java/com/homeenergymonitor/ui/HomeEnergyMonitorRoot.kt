package com.homeenergymonitor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeenergymonitor.domain.model.MeterReading
import com.homeenergymonitor.ui.components.DemoBanner
import com.homeenergymonitor.ui.dashboard.DashboardUiState
import com.homeenergymonitor.ui.dashboard.HistoryUiState
import com.homeenergymonitor.ui.screens.AnalyticsScreen
import com.homeenergymonitor.ui.screens.DevicesScreen
import com.homeenergymonitor.ui.screens.HistoryScreen
import com.homeenergymonitor.ui.screens.HomeScreen
import com.homeenergymonitor.ui.theme.Background
import com.homeenergymonitor.ui.theme.Primary
import com.homeenergymonitor.ui.theme.Surface
import com.homeenergymonitor.ui.theme.TextPrimary
import com.homeenergymonitor.ui.theme.TextSecondary

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ANALYTICS("Analytics", Icons.AutoMirrored.Filled.List),
    HISTORY("History", Icons.Filled.DateRange),
    DEVICES("Devices", Icons.Filled.Settings)
}

@Composable
fun HomeEnergyMonitorRoot(
    uiState: DashboardUiState,
    historyState: HistoryUiState,
    cachedReadings: List<MeterReading>,
    onRefresh: () -> Unit,
    onLoadInitialHistory: () -> Unit,
    onRefreshHistory: () -> Unit,
    onLoadMoreHistory: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopBar(onRefresh = onRefresh)
        },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.14f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DemoBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    Tab.HOME -> HomeScreen(
                        state = uiState,
                        onRetry = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    )

                    Tab.ANALYTICS -> AnalyticsScreen(
                        state = uiState,
                        cachedReadings = cachedReadings,
                        modifier = Modifier.fillMaxSize()
                    )

                    Tab.HISTORY -> HistoryScreen(
                        state = historyState,
                        onLoadInitial = onLoadInitialHistory,
                        onRefresh = onRefreshHistory,
                        onLoadMore = onLoadMoreHistory,
                        modifier = Modifier.fillMaxSize()
                    )

                    Tab.DEVICES -> DevicesScreen(
                        state = uiState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HomeEnergy",
                color = TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Smart meter monitor",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                tint = TextSecondary
            )
        }
    }
}
