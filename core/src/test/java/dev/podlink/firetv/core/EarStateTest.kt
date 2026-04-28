/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarStateTest {
    @Test fun bothIn_isTrue_whenBothInEar() {
        val s = EarStatus(EarState.InEar, EarState.InEar)
        assertTrue(s.bothIn)
        assertFalse(s.anyOut)
    }

    @Test fun anyOut_isTrue_whenOneOut() {
        val s = EarStatus(EarState.InEar, EarState.OutOfEar)
        assertFalse(s.bothIn)
        assertTrue(s.anyOut)
    }

    @Test fun anyOut_isFalse_whenInCase() {
        val s = EarStatus(EarState.InCase, EarState.InCase)
        assertFalse(s.anyOut)
        assertFalse(s.bothIn)
    }
}
