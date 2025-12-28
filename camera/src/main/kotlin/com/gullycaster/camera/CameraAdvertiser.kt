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

/**
 * Advertises this camera as a discoverable service on the network.
 * GullyBoss can discover and connect to this camera.
 */
class CameraAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "CameraAdvertiser"
        const val SERVICE_TYPE = "_gullycam._tcp"
    }

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false
    private var registeredName: String? = null
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    /**
     * Register this camera for discovery.
     * 
     * @param cameraId Unique ID for this camera (e.g., "camera1")
     * @param port The RTSP port this camera is serving on
     */
    fun registerCamera(cameraId: String, port: Int) {
        if (isRegistered) {
            Log.w(TAG, "Camera already registered")
            return
        }

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        multicastLock = wifiManager.createMulticastLock("cameraAdvertisingLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "GullyCam-$cameraId"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registeredName = serviceInfo.serviceName
                Log.d(TAG, "Camera registered: $registeredName on port $port")
                isRegistered = true
                showToast("Camera broadcast active: $registeredName")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: error=$errorCode")
                isRegistered = false
                showToast("Camera broadcast failed!")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Camera unregistered: ${serviceInfo.serviceName}")
                isRegistered = false
                registeredName = null
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: error=$errorCode")
            }
        }

        try {
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register camera", e)
        }
    }

    /**
     * Unregister the camera.
     */
    fun unregisterCamera() {
        if (!isRegistered || registrationListener == null) {
            return
        }

        try {
            nsdManager?.unregisterService(registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister camera", e)
        }
        
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release multicast lock", e)
        }
        multicastLock = null
    }

    fun isRegistered(): Boolean = isRegistered
    fun getRegisteredName(): String? = registeredName

    private fun showToast(msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
