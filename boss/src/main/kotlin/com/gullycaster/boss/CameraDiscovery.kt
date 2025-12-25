/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.boss

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.InetAddress

/**
 * Discovers GullyCam cameras on the network.
 */
class CameraDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "CameraDiscovery"
        const val SERVICE_TYPE = "_gullycam._tcp."
    }

    interface DiscoveryCallback {
        fun onCameraFound(name: String, host: InetAddress, port: Int)
        fun onCameraLost(name: String)
        fun onDiscoveryError(errorCode: Int)
    }

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false
    private var callback: DiscoveryCallback? = null
    
    // Track discovered cameras
    private val discoveredCameras = mutableMapOf<String, Pair<InetAddress, Int>>()

    /**
     * Start discovering cameras on the network.
     */
    fun startDiscovery(callback: DiscoveryCallback) {
        if (isDiscovering) {
            Log.w(TAG, "Already discovering")
            return
        }

        this.callback = callback
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started for $serviceType")
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Camera found: ${serviceInfo.serviceName}")
                // Resolve to get host/port
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Camera lost: ${serviceInfo.serviceName}")
                discoveredCameras.remove(serviceInfo.serviceName)
                callback.onCameraLost(serviceInfo.serviceName)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: error=$errorCode")
                isDiscovering = false
                callback.onDiscoveryError(errorCode)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: error=$errorCode")
            }
        }

        try {
            nsdManager?.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            callback.onDiscoveryError(-1)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: error=$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolved: ${serviceInfo.serviceName} -> ${serviceInfo.host}:${serviceInfo.port}")
                discoveredCameras[serviceInfo.serviceName] = Pair(serviceInfo.host, serviceInfo.port)
                callback?.onCameraFound(serviceInfo.serviceName, serviceInfo.host, serviceInfo.port)
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve service", e)
        }
    }

    /**
     * Stop discovery.
     */
    fun stopDiscovery() {
        if (!isDiscovering || discoveryListener == null) {
            return
        }

        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
        }
        isDiscovering = false
        discoveredCameras.clear()
    }

    /**
     * Get list of currently discovered cameras.
     */
    fun getDiscoveredCameras(): Map<String, Pair<InetAddress, Int>> = discoveredCameras.toMap()

    fun isDiscovering(): Boolean = isDiscovering
}
