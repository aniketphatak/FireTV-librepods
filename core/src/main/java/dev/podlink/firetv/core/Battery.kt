/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 *
 * This file is part of FireTV-librepods.
 *
 * FireTV-librepods is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Portions of the protocol model are derived from LibrePods
 * (https://github.com/kavishdevar/librepods), licensed under GPL-3.0.
 */
package dev.podlink.firetv.core

data class Battery(
    val leftPercent: Int?,
    val rightPercent: Int?,
    val casePercent: Int?,
    val leftCharging: Boolean = false,
    val rightCharging: Boolean = false,
    val caseCharging: Boolean = false,
)
