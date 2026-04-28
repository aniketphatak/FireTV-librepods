/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.bluetooth

import dev.podlink.firetv.core.EarState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleAdvertParserTest {

    /** Helper: build a 25-byte (post-header is 0x07 0x19) payload with known fields. */
    private fun build(
        modelId: Int = 0x1420, // AirPods Pro 2
        primaryLeft: Boolean = true,
        thisInCase: Boolean = false,
        leftInEar: Boolean = true,
        rightInEar: Boolean = true,
        leftPctNibble: Int = 0x9, // 90%
        rightPctNibble: Int = 0x8, // 80%
        casePctNibble: Int = 0x5, // 50%
        chargingFlags: Int = 0b0000,
        lidClosed: Boolean = false,
        color: Int = 0x00,
        connState: Int = 0x00,
    ): ByteArray {
        val status = run {
            var s = 0
            if (primaryLeft) s = s or 0x20
            if (thisInCase) s = s or 0x40
            // Encode ear bits in the layout the parser inverse-decodes.
            val xorFactor = primaryLeft xor thisInCase
            val (earBitA, earBitB) = if (xorFactor) leftInEar to rightInEar else rightInEar to leftInEar
            if (earBitA) s = s or 0x08
            if (earBitB) s = s or 0x02
            s
        }
        val highNibble = if (primaryLeft) leftPctNibble else rightPctNibble
        val lowNibble = if (primaryLeft) rightPctNibble else leftPctNibble
        val batteryByte = ((highNibble and 0x0F) shl 4) or (lowNibble and 0x0F)
        val caseByte = ((chargingFlags and 0x0F) shl 4) or (casePctNibble and 0x0F)
        val lidByte = if (lidClosed) 0x08 else 0x00

        val payload = ByteArray(11)
        payload[0] = 0x07
        payload[1] = 0x19
        payload[2] = 0x01
        payload[3] = ((modelId shr 8) and 0xFF).toByte()
        payload[4] = (modelId and 0xFF).toByte()
        payload[5] = status.toByte()
        payload[6] = batteryByte.toByte()
        payload[7] = caseByte.toByte()
        payload[8] = lidByte.toByte()
        payload[9] = color.toByte()
        payload[10] = connState.toByte()
        return payload
    }

    @Test
    fun parses_known_airpodsPro2_payload() {
        val parsed = AppleAdvertParser.parse(build())
        assertNotNull(parsed)
        assertEquals(0x1420, parsed!!.modelId)
        assertEquals("AirPods Pro 2", parsed.modelName)
        assertEquals(90, parsed.battery.leftPercent)
        assertEquals(80, parsed.battery.rightPercent)
        assertEquals(50, parsed.battery.casePercent)
        assertEquals(EarState.InEar, parsed.earStatus.left)
        assertEquals(EarState.InEar, parsed.earStatus.right)
        assertTrue(parsed.lidOpen == true)
    }

    @Test
    fun rejects_short_payload() {
        assertNull(AppleAdvertParser.parse(byteArrayOf(0x07, 0x19, 0x00)))
    }

    @Test
    fun rejects_wrong_prefix() {
        val bad = build()
        bad[0] = 0x10
        assertNull(AppleAdvertParser.parse(bad))
    }

    @Test
    fun ear_out_decodes_when_left_pod_removed() {
        val parsed = AppleAdvertParser.parse(build(leftInEar = false, rightInEar = true))
        assertNotNull(parsed)
        assertEquals(EarState.OutOfEar, parsed!!.earStatus.left)
        assertEquals(EarState.InEar, parsed.earStatus.right)
        assertTrue(parsed.earStatus.anyOut)
    }

    @Test
    fun nibble_F_means_unknown_battery() {
        val parsed = AppleAdvertParser.parse(
            build(leftPctNibble = 0xF, rightPctNibble = 0xF, casePctNibble = 0xF),
        )
        assertNotNull(parsed)
        assertNull(parsed!!.battery.leftPercent)
        assertNull(parsed.battery.rightPercent)
        assertNull(parsed.battery.casePercent)
    }

    @Test
    fun nibble_A_through_E_clamp_to_100() {
        for (n in 0xA..0xE) {
            val parsed = AppleAdvertParser.parse(build(leftPctNibble = n))!!
            assertEquals(100, parsed.battery.leftPercent)
        }
    }

    @Test
    fun primaryRight_swaps_battery_assignment() {
        // primaryLeft=false means the "high nibble" pod is the right one.
        val parsed = AppleAdvertParser.parse(
            build(
                primaryLeft = false,
                leftPctNibble = 0x3,
                rightPctNibble = 0x7,
            ),
        )!!
        assertEquals(30, parsed.battery.leftPercent)
        assertEquals(70, parsed.battery.rightPercent)
    }

    @Test
    fun unknown_model_returns_fallback_name() {
        val parsed = AppleAdvertParser.parse(build(modelId = 0xDEAD))!!
        assertEquals(0xDEAD, parsed.modelId)
        assertEquals("Unknown Apple device", parsed.modelName)
    }
}
