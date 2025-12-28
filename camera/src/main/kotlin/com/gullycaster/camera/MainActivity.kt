/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gullycaster.camera.databinding.ActivityMainBinding
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gullycaster.common.Constants
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtspserver.RtspServerCamera2
import com.pedro.common.ConnectChecker
import com.pedro.library.view.OpenGlView
import java.util.UUID

/**
 * GullyCam - Main camera streaming activity.
 */
class MainActivity : AppCompatActivity(), ConnectChecker, SurfaceHolder.Callback {

    companion object {
        private const val RTSP_PORT = 8554
    }

    private lateinit var binding: ActivityMainBinding
    private var rtspServer: RtspServerCamera2? = null
    private var isSurfaceReady = false
    private lateinit var cameraAdvertiser: CameraAdvertiser
    private val cameraId = UUID.randomUUID().toString().take(8)
    private var connectedClients = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true
        ) {
            initCameraIfReady()
        } else {
            showToast("Camera and Audio permissions are required!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.surfaceView.holder.addCallback(this)
        cameraAdvertiser = CameraAdvertiser(this)
        setupUI()
        checkPermissions()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        initCameraIfReady()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        stopServerSafely()
    }

    private fun initCameraIfReady() {
        if (!isSurfaceReady) return
        if (!hasPermissions(this, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) return
        
        if (rtspServer == null) {
            // Using the constructor: constructor(openGlView: OpenGlView, connectChecker: ConnectChecker, port: Int)
            rtspServer = RtspServerCamera2(binding.surfaceView as OpenGlView, this, RTSP_PORT)
        }
        startCameraPreview()
    }

    private fun checkPermissions() {
        if (!hasPermissions(this, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else {
            initCameraIfReady()
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = 
        permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun startCameraPreview() {
        try {
            if (rtspServer?.isStreaming == false && rtspServer?.isOnPreview == false) {
                rtspServer?.startPreview(CameraHelper.Facing.BACK, 640, 480)
            }
        } catch (e: Exception) {
            showToast("Error starting preview: ${e.message}")
        }
    }

    private fun stopServerSafely() {
        try {
            if (rtspServer?.isStreaming == true) rtspServer?.stopStream()
            if (rtspServer?.isOnPreview == true) rtspServer?.stopPreview()
        } catch (e: Exception) {}
    }

    private fun setupUI() {
        binding.btnConnect.text = "Start Server"
        binding.btnConnect.setOnClickListener {
            if (rtspServer?.isStreaming == true) stopServer() else startServer()
        }
        binding.btnEcoMode.setOnClickListener { toggleEcoMode() }
        binding.zoomSlider.addOnChangeListener { _, value, _ ->
            try { rtspServer?.setZoom(value) } catch (e: Exception) {}
        }
        updateStatus()
    }

    private fun startServer() {
        try {
            // prepareVideo overload: (width, height, fps, bitrate, hardwareRotation, rotation)
            if (rtspServer?.prepareAudio() == true && 
                rtspServer?.prepareVideo(640, 480, 20, 800 * 1024, 0) == true) {
                
                rtspServer?.startStream("live")
                cameraAdvertiser.registerCamera(cameraId, RTSP_PORT)
                binding.btnConnect.text = "Stop Server"
                binding.tallyBorder.visibility = View.VISIBLE
                updateStatus()
                showToast("Stream Server Active & Broadcasting")
            } else {
                showToast("Error preparing stream")
            }
        } catch (e: Exception) {
            showToast("Error starting server: ${e.message}")
        }
    }

    private fun stopServer() {
        try {
            cameraAdvertiser.unregisterCamera()
            rtspServer?.stopStream()
        } catch (e: Exception) {}
        binding.btnConnect.text = "Start Server"
        binding.tallyBorder.visibility = View.GONE
        connectedClients = 0
        updateStatus()
    }

    private fun updateStatus() {
        val ip = getLocalIpAddress()
        if (rtspServer?.isStreaming == true) {
            binding.statusText.text = "RTSP Server Active\nrtsp://$ip:$RTSP_PORT\nClients: $connectedClients"
        } else {
            binding.statusText.text = "Ready to start\nHotspot IP: $ip"
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        } catch (e: Exception) { "Unknown" }
    }
    
    private var isEcoMode = false
    private fun toggleEcoMode() {
        isEcoMode = !isEcoMode
        val layout = window.attributes
        layout.screenBrightness = if (isEcoMode) 0.01f else -1f
        window.attributes = layout
        binding.btnEcoMode.text = if (isEcoMode) "Normal Mode" else getString(R.string.mode_eco)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        initCameraIfReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraAdvertiser.unregisterCamera()
        stopServerSafely()
        rtspServer = null
    }

    // ConnectChecker Implementation
    override fun onConnectionSuccess() {
        runOnUiThread {
            connectedClients++
            updateStatus()
            showToast("Client connected! ($connectedClients)")
        }
    }
    override fun onConnectionFailed(reason: String) {
        runOnUiThread { showToast("Connection Failed: $reason") }
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() {
        runOnUiThread {
            connectedClients = maxOf(0, connectedClients - 1)
            updateStatus()
            showToast("Client disconnected")
        }
    }
    override fun onAuthError() { runOnUiThread { showToast("Auth Error") } }
    override fun onAuthSuccess() { runOnUiThread { showToast("Auth Success") } }
    override fun onConnectionStarted(url: String) {}
}
