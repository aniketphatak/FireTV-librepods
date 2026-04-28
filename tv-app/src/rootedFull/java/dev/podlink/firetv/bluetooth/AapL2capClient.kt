/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * AAP-over-L2CAP client wrapper for the rootedFull flavor.
 * The real implementation will reuse upstream LibrePods native code at
 * third_party/librepods/android/app/src/main/cpp and Kotlin glue under
 * third_party/librepods/android/app/src/main/java/me/kavishdevar/librepods/bluetooth.
 */
package dev.podlink.firetv.bluetooth

object AapL2capClient {
    fun isAvailable(): Boolean {
        // Phase 1: stub. Phase 5 will check for root + privileged permissions.
        return false
    }
}
