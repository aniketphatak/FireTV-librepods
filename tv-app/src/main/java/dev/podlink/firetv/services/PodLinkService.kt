/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.podlink.firetv.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dev.podlink.firetv.PodLinkRepository
import dev.podlink.firetv.PodLinkRepository.ServiceStatus
import dev.podlink.firetv.R
import dev.podlink.firetv.audio.AudioRouter
import dev.podlink.firetv.audio.PlaybackPauser
import dev.podlink.firetv.bluetooth.BleScanner

/**
 * Foreground service that owns:
 *  - the BLE scanner (advert-based connection / battery / ear-state),
 *  - the audio router (logs A2DP routing events),
 *  - the playback pauser (auto pause/resume on ear removal).
 *
 * Every dangerous call in [onCreate] is wrapped so that a single
 * subsystem failure surfaces as an error in the UI (via
 * [PodLinkRepository.setError]) instead of crashing the whole app.
 */
class PodLinkService : LifecycleService() {

    private var scanner: BleScanner? = null
    private var audioRouter: AudioRouter? = null
    private var playbackPauser: PlaybackPauser? = null
    private var foregroundStarted: Boolean = false

    override fun onCreate() {
        super.onCreate()
        PodLinkRepository.setServiceStatus(ServiceStatus.Starting)

        // 1. Foreground notification — must succeed within 5 s of
        // startForegroundService, or the framework crashes us with
        // ForegroundServiceDidNotStartInTimeException.
        val foregroundOk = try {
            ensureChannel()
            startForeground(NOTIFICATION_ID, buildNotification(), foregroundType())
            true
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            PodLinkRepository.setError(
                "Foreground service start failed: ${shortException(t)}",
            )
            // We bail here because Android will kill the service shortly anyway.
            stopSelfSafe()
            false
        }
        if (!foregroundOk) return
        foregroundStarted = true

        // 2. PlaybackPauser — pure construction, should never throw,
        // but guard anyway.
        playbackPauser = runCatching { PlaybackPauser(this) }
            .onFailure {
                Log.e(TAG, "PlaybackPauser init failed", it)
                PodLinkRepository.setError("PlaybackPauser init: ${shortException(it)}")
            }.getOrNull()

        // 3. AudioRouter — registers a Bluetooth broadcast receiver,
        // which on API 33+ requires RECEIVER_NOT_EXPORTED and on
        // API 34+ may need BLUETOOTH_CONNECT for the Bluetooth
        // intent actions.
        audioRouter = runCatching {
            AudioRouter(this).also { it.start() }
        }.onFailure {
            Log.e(TAG, "AudioRouter start failed", it)
            // Non-fatal: scanner can still run.
            PodLinkRepository.setRoutingNote(
                "Audio router unavailable: ${shortException(it)}",
            )
        }.getOrNull()

        // 4. BLE scanner — the actual feature. If this fails we cannot
        // do anything useful, so surface the error prominently.
        scanner = BleScanner(
            context = this,
            onParsed = { result, parsed ->
                val name = result.scanRecord?.deviceName
                    ?: runCatching { result.device.name }.getOrNull()
                    ?: parsed.modelName
                PodLinkRepository.onAdvertParsed(
                    deviceName = name,
                    deviceAddress = result.device.address ?: "",
                    battery = parsed.battery,
                    earStatus = parsed.earStatus,
                    nowMs = System.currentTimeMillis(),
                )
                playbackPauser?.onEarStatus(parsed.earStatus)
            },
            onUnavailable = { reason ->
                Log.w(TAG, "Scanner unavailable: $reason")
                PodLinkRepository.setError("Scanner unavailable: $reason")
            },
        )

        val started = runCatching { scanner?.start() == true }
            .onFailure {
                Log.e(TAG, "scanner.start threw", it)
                PodLinkRepository.setError("Scanner start: ${shortException(it)}")
            }.getOrDefault(false)

        if (started) {
            PodLinkRepository.setServiceStatus(ServiceStatus.Scanning)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { scanner?.stop() }
        runCatching { audioRouter?.stop() }
        runCatching { playbackPauser?.reset() }
        if (PodLinkRepository.state.value.serviceStatus != ServiceStatus.Failed) {
            PodLinkRepository.setServiceStatus(ServiceStatus.Stopped)
        }
        super.onDestroy()
    }

    private fun stopSelfSafe() {
        runCatching { stopSelf() }
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_idle))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun foregroundType(): Int =
        when {
            // API 34+ requires the explicit type to match what the
            // manifest declares; the constant CONNECTED_DEVICE has
            // existed since API 30.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            // API 30-33 accepts the typed overload but is happy with 0
            // (which means "use the manifest's declared type").
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            else -> 0
        }

    companion object {
        private const val TAG = "PodLink/Service"
        private const val CHANNEL_ID = "podlink_status"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, PodLinkService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "startForegroundService threw", t)
                PodLinkRepository.setError(
                    "Cannot start service: ${shortException(t)}",
                )
            }
        }

        private fun shortException(t: Throwable): String {
            val name = t.javaClass.simpleName
            val msg = t.message?.take(160) ?: ""
            return if (msg.isBlank()) name else "$name — $msg"
        }
    }
}
