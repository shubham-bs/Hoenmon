package com.homeenergymonitor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay

fun formatNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)

@Composable
fun rememberNow(intervalMillis: Long = 1_000L): State<Instant> =
    produceState(initialValue = Instant.now()) {
        while (true) {
            delay(intervalMillis)
            value = Instant.now()
        }
    }
