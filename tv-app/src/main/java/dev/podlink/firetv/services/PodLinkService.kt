/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Orchestration informed by upstream LibrePods'
 * services/AirPodsService.kt (GPL-3.0). Phone-only concerns (call
 * answer, quick-settings tile, app widgets) are intentionally omitted.
 */
package dev.podlink.firetv.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
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
 * State is published via [PodLinkRepository] so the UI can observe it.
 */
class PodLinkService : LifecycleService() {

    private lateinit var scanner: BleScanner
    private lateinit var audioRouter: AudioRouter
    private lateinit var playbackPauser: PlaybackPauser

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(), foregroundType())

        playbackPauser = PlaybackPauser(this)
        audioRouter = AudioRouter(this).also { it.start() }
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
                playbackPauser.onEarStatus(parsed.earStatus)
            },
            onUnavailable = { reason ->
                PodLinkRepository.setRoutingNote("Scanner: $reason")
            },
        )

        if (scanner.start()) {
            PodLinkRepository.setServiceStatus(ServiceStatus.Scanning)
        } else {
            PodLinkRepository.setServiceStatus(ServiceStatus.Stopped)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        scanner.stop()
        audioRouter.stop()
        playbackPauser.reset()
        PodLinkRepository.setServiceStatus(ServiceStatus.Stopped)
        super.onDestroy()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

    @Suppress("unused")
    private fun startedAtMs(): Long = SystemClock.uptimeMillis()

    companion object {
        private const val CHANNEL_ID = "podlink_status"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, PodLinkService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
