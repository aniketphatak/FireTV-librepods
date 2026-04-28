/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.podlink.firetv.PodLinkRepository
import dev.podlink.firetv.core.Battery
import dev.podlink.firetv.core.EarState
import dev.podlink.firetv.core.EarStatus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(state: PodLinkRepository.State) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "PodLink",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "Status: ${state.serviceStatus}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.deviceName ?: "Searching for AirPods…",
                style = MaterialTheme.typography.headlineSmall,
            )
            BatteryRow(state.battery)
            EarRow(state.earStatus)
            Text(
                text = "Pause events: ${state.pauseEventCount}",
                style = MaterialTheme.typography.bodyMedium,
            )
            state.routingNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BatteryRow(battery: Battery?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryBadge("Left", battery?.leftPercent, battery?.leftCharging == true)
        BatteryBadge("Right", battery?.rightPercent, battery?.rightCharging == true)
        BatteryBadge("Case", battery?.casePercent, battery?.caseCharging == true)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BatteryBadge(label: String, percent: Int?, charging: Boolean) {
    val text = percent?.let { "$it%" } ?: "—"
    Box(
        modifier = Modifier.width(160.dp).padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(text, style = MaterialTheme.typography.headlineMedium)
            Text(
                if (charging) "charging" else " ",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EarRow(status: EarStatus) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Left:  ${describe(status.left)}", style = MaterialTheme.typography.bodyLarge)
        Text("Right: ${describe(status.right)}", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun describe(state: EarState): String = when (state) {
    EarState.InEar -> "in ear"
    EarState.OutOfEar -> "out"
    EarState.InCase -> "in case"
    EarState.Unknown -> "unknown"
}
