/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.bluetooth

import dev.podlink.firetv.core.Battery
import dev.podlink.firetv.core.EarState
import dev.podlink.firetv.core.EarStatus

/**
 * Parses Apple manufacturer-specific BLE advertisement payloads (company
 * id 0x004C, "continuity" protocol) to derive battery and ear-state.
 * Phase 1 stub; the real packet layout will be filled in during phase 2
 * with reference to the upstream `bluetooth/` package and the public
 * reverse-engineering notes at third_party/librepods/AAP Definitions.md.
 */
object AdvertisementParser {
    data class Result(val battery: Battery?, val ears: EarStatus?)

    fun parse(@Suppress("UNUSED_PARAMETER") manufacturerData: ByteArray): Result {
        return Result(
            battery = null,
            ears = EarStatus(EarState.Unknown, EarState.Unknown),
        )
    }
}
