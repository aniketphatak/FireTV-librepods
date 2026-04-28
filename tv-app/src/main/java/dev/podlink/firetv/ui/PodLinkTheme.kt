/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PodLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
}
