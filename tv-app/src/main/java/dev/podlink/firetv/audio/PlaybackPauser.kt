/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Uses the same `AudioManager.dispatchMediaKeyEvent` pattern as upstream
 * LibrePods' utils/MediaController.kt (GPL-3.0). That call works on
 * Fire OS / stock Android without root and without
 * BIND_NOTIFICATION_LISTENER_SERVICE.
 */
package dev.podlink.firetv.audio

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import dev.podlink.firetv.PodLinkRepository
import dev.podlink.firetv.core.EarStatus

/**
 * Pauses Fire TV media playback when an ear is removed and resumes when
 * both buds are back in the ear. Includes a debounce so flapping
 * advertisements don't toggle playback aggressively.
 */
class PlaybackPauser(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var lastReportedAnyOut: Boolean? = null
    private var lastActionUptimeMs: Long = 0L

    /** Was the most recent automatic action a pause initiated by us? */
    private var weArePaused: Boolean = false

    /**
     * Called for each new [EarStatus] received from the parser.
     * Implements a simple "edge detector":
     *  - any-out -> both-in transition: dispatch PLAY (only if we paused)
     *  - both-in -> any-out transition: dispatch PAUSE
     *
     * Transitions are gated on [DEBOUNCE_MS] since the last action.
     */
    fun onEarStatus(status: EarStatus) {
        val anyOut = status.anyOut
        val bothIn = status.bothIn

        val previous = lastReportedAnyOut
        // Initialize baseline on first observation; never act on it.
        if (previous == null) {
            lastReportedAnyOut = anyOut
            return
        }
        if (previous == anyOut) return

        val now = SystemClock.uptimeMillis()
        if (now - lastActionUptimeMs < DEBOUNCE_MS) return

        when {
            anyOut && !previous -> {
                dispatch(KeyEvent.KEYCODE_MEDIA_PAUSE)
                weArePaused = true
                Log.i(TAG, "Ear removed -> PAUSE")
                PodLinkRepository.incrementPauseCount()
                lastActionUptimeMs = now
            }
            bothIn && previous && weArePaused -> {
                dispatch(KeyEvent.KEYCODE_MEDIA_PLAY)
                weArePaused = false
                Log.i(TAG, "Both ears in -> PLAY")
                lastActionUptimeMs = now
            }
        }
        lastReportedAnyOut = anyOut
    }

    private fun dispatch(keycode: Int) {
        runCatching {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
        }.onFailure { Log.w(TAG, "dispatchMediaKeyEvent failed", it) }
    }

    fun reset() {
        lastReportedAnyOut = null
        lastActionUptimeMs = 0L
        weArePaused = false
    }

    companion object {
        private const val TAG = "PodLink/PlaybackPauser"
        private const val DEBOUNCE_MS = 800L
    }
}
