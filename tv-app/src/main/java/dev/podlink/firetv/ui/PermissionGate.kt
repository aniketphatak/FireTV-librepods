/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGate(
    permissions: List<String>,
    onGrantClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "PodLink needs Bluetooth permission",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                "PodLink scans for nearby AirPods to show their battery and " +
                    "pause Fire TV playback when you take a bud out.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Required: ${permissions.joinToString(", ") { it.substringAfterLast('.') }}",
                style = MaterialTheme.typography.labelMedium,
            )
            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable(),
            ) {
                Text("Grant permission")
            }
        }
    }
}
