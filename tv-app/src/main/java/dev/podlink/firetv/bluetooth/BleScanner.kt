/*
 * Copyright (C) 2026 The FireTV-librepods authors.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Scan-filter setup is informed by upstream LibrePods'
 * bluetooth/BLEManager.kt (GPL-3.0).
 */
package dev.podlink.firetv.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Scans for Apple manufacturer-specific BLE advertisements (company id
 * `0x004C`, type `0x07`, length `0x19`) and forwards parsed results to
 * [onParsed].
 *
 * Caller is responsible for ensuring the relevant runtime permissions
 * are granted before [start] is invoked. [start] returns `false` if it
 * cannot begin scanning for any reason; the failure reason is logged.
 */
class BleScanner(
    private val context: Context,
    private val onParsed: (ScanResult, AppleAdvertParser.Parsed) -> Unit,
    private val onUnavailable: (String) -> Unit = {},
) {
    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (scanCallback != null) return true

        if (!hasScanPermission()) {
            onUnavailable("BLUETOOTH_SCAN permission not granted")
            return false
        }

        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter
        if (adapter == null || !adapter.isEnabled) {
            onUnavailable("Bluetooth is off")
            return false
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onUnavailable("BLE scanner unavailable")
            return false
        }

        val filter = ScanFilter.Builder()
            .setManufacturerData(
                AppleAdvertParser.APPLE_COMPANY_ID,
                AppleAdvertParser.PREFIX,
                MANUFACTURER_MASK,
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
                    setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                }
            }
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handle(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::handle)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "BLE scan failed: code=$errorCode")
                onUnavailable("Scan failed (code $errorCode)")
            }
        }

        return try {
            scanner.startScan(listOf(filter), settings, cb)
            scanCallback = cb
            Log.i(TAG, "BLE scan started")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "startScan threw", t)
            onUnavailable(t.message ?: "Scan start exception")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val cb = scanCallback ?: return
        scanCallback = null
        if (!hasScanPermission()) return
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (t: Throwable) {
            Log.w(TAG, "stopScan threw", t)
        }
    }

    private fun handle(result: ScanResult) {
        val mfg = result.scanRecord?.getManufacturerSpecificData(AppleAdvertParser.APPLE_COMPANY_ID)
            ?: return
        val parsed = AppleAdvertParser.parse(mfg) ?: return
        onParsed(result, parsed)
    }

    private fun hasScanPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "PodLink/BleScanner"

        // Match the prefix bytes exactly (0xFF, 0xFF) — anything past those is data.
        private val MANUFACTURER_MASK = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
    }
}
