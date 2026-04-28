/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv

import dev.podlink.firetv.core.Battery
import dev.podlink.firetv.core.EarState
import dev.podlink.firetv.core.EarStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide state holder shared between [services.PodLinkService] (writer)
 * and [MainActivity] (reader). Exposed as a [StateFlow] so Compose can
 * collect it directly.
 */
object PodLinkRepository {

    enum class ServiceStatus { Stopped, Starting, Scanning, Connected, Failed }

    data class State(
        val serviceStatus: ServiceStatus = ServiceStatus.Stopped,
        val deviceName: String? = null,
        val deviceAddress: String? = null,
        val battery: Battery? = null,
        val earStatus: EarStatus = EarStatus(EarState.Unknown, EarState.Unknown),
        val lastSeenAtMs: Long? = null,
        val routingNote: String? = null,
        val pauseEventCount: Int = 0,
        val errorMessage: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setServiceStatus(status: ServiceStatus) =
        _state.update { it.copy(serviceStatus = status) }

    fun setError(message: String) = _state.update {
        it.copy(serviceStatus = ServiceStatus.Failed, errorMessage = message)
    }

    fun onAdvertParsed(
        deviceName: String,
        deviceAddress: String,
        battery: Battery,
        earStatus: EarStatus,
        nowMs: Long,
    ) = _state.update {
        it.copy(
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            battery = battery,
            earStatus = earStatus,
            lastSeenAtMs = nowMs,
            serviceStatus = ServiceStatus.Connected,
        )
    }

    fun onConnectionLost() = _state.update {
        it.copy(
            serviceStatus = if (it.serviceStatus == ServiceStatus.Connected)
                ServiceStatus.Scanning else it.serviceStatus,
        )
    }

    fun setRoutingNote(note: String?) =
        _state.update { it.copy(routingNote = note) }

    fun incrementPauseCount() =
        _state.update { it.copy(pauseEventCount = it.pauseEventCount + 1) }
}
