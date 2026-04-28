/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * The orchestration logic in this file is informed by upstream LibrePods'
 * services/AirPodsService.kt (https://github.com/kavishdevar/librepods,
 * GPL-3.0). Phone-only concerns (call answer, quick-settings tile, app
 * widgets) are intentionally omitted for the Fire TV port.
 */
package dev.podlink.firetv.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dev.podlink.firetv.R

/**
 * Foreground service that owns the Bluetooth lifecycle and emits
 * [dev.podlink.firetv.core.PodEvent]s. Phase 1 is a connected-device
 * stub so we can validate the manifest entry and notification channel
 * on real hardware before wiring real Bluetooth code in phase 2.
 */
class PodLinkService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(), foregroundType())
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

    companion object {
        private const val CHANNEL_ID = "podlink_status"
        private const val NOTIFICATION_ID = 1
    }
}
