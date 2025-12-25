/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.camera

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.gullycaster.common.Constants
import java.net.InetAddress

/**
 * Discovers GullyBoss service on the local network using mDNS/NSD.
 */
class NsdDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "NsdDiscovery"
    }

    interface DiscoveryCallback {
        fun onServiceFound(host: InetAddress, port: Int)
        fun onServiceLost()
        fun onDiscoveryFailed(errorCode: Int)
    }

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null
    private var isDiscovering = false
    private var callback: DiscoveryCallback? = null

    /**
     * Start discovering GullyBoss services on the network.
     */
    fun startDiscovery(callback: DiscoveryCallback) {
        if (isDiscovering) {
            Log.w(TAG, "Already discovering")
            return
        }

        this.callback = callback
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed: error=$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: ${serviceInfo.host}:${serviceInfo.port}")
                callback.onServiceFound(serviceInfo.host, serviceInfo.port)
            }
        }

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
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains(Constants.NSD_SERVICE_NAME)) {
                    // Resolve the service to get host/port
                    try {
                        nsdManager?.resolveService(serviceInfo, resolveListener)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve service", e)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains(Constants.NSD_SERVICE_NAME)) {
                    callback.onServiceLost()
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: error=$errorCode")
                isDiscovering = false
                callback.onDiscoveryFailed(errorCode)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: error=$errorCode")
            }
        }

        try {
            nsdManager?.discoverServices(
                Constants.NSD_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            callback.onDiscoveryFailed(-1)
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
    }

    fun isDiscovering(): Boolean = isDiscovering
}
