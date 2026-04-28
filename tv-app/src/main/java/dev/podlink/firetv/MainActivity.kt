/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.podlink.firetv.ui.PodLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PodLinkTheme { Home() } }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Home() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "PodLink for Fire TV",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "Connect Apple AirPods or other Bluetooth earbuds to your Fire TV.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Distribution: ${BuildConfig.DISTRIBUTION_CHANNEL}  •  " +
                    "Root: ${if (BuildConfig.REQUIRES_ROOT) "required" else "not required"}",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
