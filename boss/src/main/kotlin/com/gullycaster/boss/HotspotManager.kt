/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.boss

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manages the Wi-Fi Hotspot for GullyBoss.
 * 
 * Uses WifiManager.LocalOnlyHotspotCallback (API 26+) to create
 * a local-only hotspot that camera devices can connect to.
 */
class HotspotManager(private val context: Context) {

    companion object {
        private const val TAG = "HotspotManager"
    }

    interface HotspotCallback {
        fun onHotspotStarted(ssid: String, password: String)
        fun onHotspotStopped()
        fun onHotspotFailed(reason: Int)
    }

    private var callback: HotspotCallback? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Start a local-only hotspot.
     * 
     * Note: This requires CHANGE_WIFI_STATE permission and
     * Location Services to be enabled on Android 8.0+.
     */
    fun startHotspot(callback: HotspotCallback) {
        this.callback = callback
        
        try {
            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        hotspotReservation = reservation
                        val config = reservation?.wifiConfiguration
                        val softApConfig = reservation?.softApConfiguration
                        
                        // Handle different API levels
                        val ssid: String
                        val password: String
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && softApConfig != null) {
                            ssid = softApConfig.ssid ?: "Unknown"
                            password = softApConfig.passphrase ?: ""
                        } else if (config != null) {
                            @Suppress("DEPRECATION")
                            ssid = config.SSID ?: "Unknown"
                            @Suppress("DEPRECATION")
                            password = config.preSharedKey ?: ""
                        } else {
                            ssid = "Unknown"
                            password = ""
                        }
                        
                        Log.d(TAG, "Hotspot started: SSID=$ssid")
                        callback.onHotspotStarted(ssid, password)
                    }

                    override fun onStopped() {
                        Log.d(TAG, "Hotspot stopped")
                        hotspotReservation = null
                        callback.onHotspotStopped()
                    }

                    override fun onFailed(reason: Int) {
                        Log.e(TAG, "Hotspot failed: reason=$reason")
                        callback.onHotspotFailed(reason)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting hotspot", e)
            callback.onHotspotFailed(-1)
        }
    }

    /**
     * Stop the hotspot and release the reservation.
     */
    fun stopHotspot() {
        try {
            hotspotReservation?.close()
            hotspotReservation = null
            callback?.onHotspotStopped()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping hotspot", e)
        }
    }

    /**
     * Check if hotspot is currently active.
     */
    fun isHotspotActive(): Boolean = hotspotReservation != null
}
