/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.core

enum class EarState { InEar, OutOfEar, InCase, Unknown }

data class EarStatus(val left: EarState, val right: EarState) {
    val anyOut: Boolean
        get() = left == EarState.OutOfEar || right == EarState.OutOfEar
    val bothIn: Boolean
        get() = left == EarState.InEar && right == EarState.InEar
}
