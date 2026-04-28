/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dev.podlink.firetv.PodLinkRepository

/**
 * Observes Bluetooth A2DP / ACL connect-disconnect events and nudges the
 * Fire TV media output toward connected AirPods. Fire OS routes A2DP
 * audio automatically in most cases; this class exists to (a) record
 * the routing event for the UI and (b) explicitly clear the
 * communication device on disconnect so playback doesn't stall.
 */
class AudioRouter(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDisconnected(device)
            }
        }
    }

    private var registered = false

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    private fun onConnected(device: BluetoothDevice?) {
        val name = device?.let { runCatching { it.name }.getOrNull() } ?: "earbuds"
        Log.i(TAG, "ACL connected: $name")
        PodLinkRepository.setRoutingNote("Audio routed to $name")
    }

    private fun onDisconnected(device: BluetoothDevice?) {
        val name = device?.let { runCatching { it.name }.getOrNull() } ?: "earbuds"
        Log.i(TAG, "ACL disconnected: $name")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.clearCommunicationDevice() }
        }
        PodLinkRepository.setRoutingNote("Audio fell back to TV speakers")
    }

    @Suppress("unused")
    private fun bluetoothAdapter(): BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    companion object {
        private const val TAG = "PodLink/AudioRouter"
    }
}
