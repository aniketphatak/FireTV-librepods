/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Byte-layout knowledge in this file is derived from
 *   third_party/librepods/android/app/src/main/java/me/kavishdevar/librepods/bluetooth/BLEManager.kt
 * (LibrePods, GPL-3.0). The unencrypted Apple "continuity / proximity"
 * advertisement format (manufacturer id 0x004C, type 0x07) is also
 * documented across multiple public reverse-engineering writeups.
 */
package dev.podlink.firetv.bluetooth

import dev.podlink.firetv.core.Battery
import dev.podlink.firetv.core.EarState
import dev.podlink.firetv.core.EarStatus

/**
 * Parser for Apple manufacturer-specific BLE advertisements (company id
 * `0x004C`) that AirPods broadcast even when not connected over GATT.
 *
 * The advertisement is emitted with a leading type byte `0x07` and
 * length byte `0x19` (filtered for in [BleScanner]). The remaining 25
 * bytes carry an unencrypted summary of the AirPods' state.
 *
 * Byte layout (after the `0x07 0x19` prefix is consumed):
 * ```
 * [0]  0x07 (type)
 * [1]  0x19 (length)
 * [2]  pairing flag
 * [3]  model id high byte
 * [4]  model id low  byte
 * [5]  status: bit5=primaryLeft, bit6=thisInCase, bits3 & 1 = ear state
 * [6]  battery (high nibble = pod A, low nibble = pod B)
 * [7]  case battery (low nibble) + charging flags (high nibble)
 * [8]  lid state (bit 3)
 * [9]  color
 * [10] connection state
 * ```
 */
object AppleAdvertParser {

    /** Apple Bluetooth SIG company identifier. */
    const val APPLE_COMPANY_ID: Int = 0x004C

    /** First two bytes of the manufacturer-specific payload for AirPods proximity messages. */
    val PREFIX: ByteArray = byteArrayOf(0x07, 0x19)

    data class Parsed(
        val modelId: Int,
        val modelName: String,
        val battery: Battery,
        val earStatus: EarStatus,
        val lidOpen: Boolean?,
    )

    /**
     * Parses a full manufacturer-specific payload (including the leading
     * `0x07 0x19` prefix). Returns `null` if the payload is too short
     * or does not match the expected prefix.
     */
    fun parse(payload: ByteArray): Parsed? {
        if (payload.size < 11) return null
        if (payload[0] != PREFIX[0] || payload[1] != PREFIX[1]) return null

        val modelId = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF)
        val status = payload[5].toInt() and 0xFF
        val batteryByte = payload[6].toInt() and 0xFF
        val caseByte = payload[7].toInt() and 0xFF
        val lidByte = payload[8].toInt() and 0xFF

        val primaryLeft = (status and 0x20) != 0
        val thisInCase = (status and 0x40) != 0
        val xorFactor = primaryLeft xor thisInCase

        // Pod battery: high nibble is the "primary" pod, low is the "secondary".
        // primaryLeft true => high nibble = left, low nibble = right.
        val highNibblePct = decodeBatteryNibble((batteryByte shr 4) and 0x0F)
        val lowNibblePct = decodeBatteryNibble(batteryByte and 0x0F)
        val leftPct = if (primaryLeft) highNibblePct else lowNibblePct
        val rightPct = if (primaryLeft) lowNibblePct else highNibblePct

        val casePct = decodeBatteryNibble(caseByte and 0x0F)
        val chargingFlags = (caseByte shr 4) and 0x0F
        val leftCharging = (chargingFlags and 0b0001) != 0
        val rightCharging = (chargingFlags and 0b0010) != 0
        val caseCharging = (chargingFlags and 0b0100) != 0

        // Ear bits: depend on xorFactor (which side is primary AND which is in-case).
        val earBitA = (status and 0x08) != 0
        val earBitB = (status and 0x02) != 0
        val (leftIn, rightIn) = if (xorFactor) earBitA to earBitB else earBitB to earBitA

        val lidOpen = ((lidByte and 0x08) != 0).not()

        val left = earSlot(leftIn, thisInCase && primaryLeft)
        val right = earSlot(rightIn, thisInCase && !primaryLeft)

        return Parsed(
            modelId = modelId,
            modelName = MODEL_NAMES[modelId] ?: "Unknown Apple device",
            battery = Battery(
                leftPercent = leftPct,
                rightPercent = rightPct,
                casePercent = casePct,
                leftCharging = leftCharging,
                rightCharging = rightCharging,
                caseCharging = caseCharging,
            ),
            earStatus = EarStatus(left, right),
            lidOpen = lidOpen,
        )
    }

    private fun earSlot(inEarBit: Boolean, podInCase: Boolean): EarState = when {
        podInCase -> EarState.InCase
        inEarBit -> EarState.InEar
        else -> EarState.OutOfEar
    }

    private fun decodeBatteryNibble(nibble: Int): Int? = when (nibble) {
        in 0x0..0x9 -> nibble * 10
        in 0xA..0xE -> 100
        else -> null // 0xF = unknown
    }

    private val MODEL_NAMES: Map<Int, String> = mapOf(
        0x0E20 to "AirPods Pro",
        0x1420 to "AirPods Pro 2",
        0x2420 to "AirPods Pro 2 (USB-C)",
        0x0220 to "AirPods 1",
        0x0F20 to "AirPods 2",
        0x1320 to "AirPods 3",
        0x1920 to "AirPods 4",
        0x1B20 to "AirPods 4 (ANC)",
        0x0A20 to "AirPods Max",
        0x1F20 to "AirPods Max (USB-C)",
    )
}
