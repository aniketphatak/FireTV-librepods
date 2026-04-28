/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.core

/**
 * Flavor-agnostic events emitted by the Bluetooth integration layer
 * (BLE-advertisement-based on [appstoreLite], AAP/L2CAP on [rootedFull])
 * and consumed by the foreground service / UI.
 */
sealed interface PodEvent {
    data class Connected(val name: String, val address: String) : PodEvent
    data object Disconnected : PodEvent
    data class BatteryUpdate(val battery: Battery) : PodEvent
    data class EarUpdate(val status: EarStatus) : PodEvent
}
