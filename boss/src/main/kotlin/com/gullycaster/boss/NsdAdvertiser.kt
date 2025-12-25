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
import com.gullycaster.common.Constants

/**
 * Manages NSD (Network Service Discovery) for advertising
 * the GullyBoss service so cameras can discover it automatically.
 */
class NsdAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "NsdAdvertiser"
    }

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    /**
     * Register the GullyBoss service for discovery.
     * 
     * @param port The port number where the RTMP server is running
     */
    fun registerService(port: Int) {
        if (isRegistered) {
            Log.w(TAG, "Service already registered")
            return
        }

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = Constants.NSD_SERVICE_NAME
            serviceType = Constants.NSD_SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${serviceInfo.serviceName}")
                isRegistered = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: error=$errorCode")
                isRegistered = false
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                isRegistered = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: error=$errorCode")
            }
        }

        try {
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register service", e)
        }
    }

    /**
     * Unregister the service.
     */
    fun unregisterService() {
        if (!isRegistered || registrationListener == null) {
            return
        }

        try {
            nsdManager?.unregisterService(registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister service", e)
        }
    }
}
