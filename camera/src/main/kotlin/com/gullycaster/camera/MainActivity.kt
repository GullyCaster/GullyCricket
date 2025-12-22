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
import android.graphics.Color
import android.view.View
import android.view.WindowManager

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.common.ConnectChecker

/**
 * GullyCam - Main camera streaming activity.
 * 
 * This activity provides:
 * - Camera preview with zoom controls
 * - RTMP/SRT streaming to GullyBoss
 * - Eco-mode for thermal efficiency
 * - Tally light indicator
 */
class MainActivity : AppCompatActivity(), ConnectChecker {


    // UI Binding
    private lateinit var binding: ActivityMainBinding

    // RTMP Streaming
    private var rtmpCamera2: RtmpCamera2? = null

    // Permission Handling
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true
        ) {
            startCameraPreview()
        } else {
            showToast("Camera and Audio permissions are required!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on while streaming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStreaming()
        setupUI()
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!hasPermissions(this, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        } else {
            startCameraPreview()
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = 
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun setupStreaming() {
        rtmpCamera2 = RtmpCamera2(binding.surfaceView, this)
        // isAutoHandleOrientation might not be directly available or needed if OpenGlView handles it.
        // Checking for different orientation handling if needed, but removing the error line for now.
    }

    private fun startCameraPreview() {
        if (rtmpCamera2?.isStreaming == false && !rtmpCamera2!!.isOnPreview) {
            // Default to 1920x1080, 30fps
            rtmpCamera2?.startPreview(1920, 1080)
        }
    }

    private fun setupUI() {
        // Connect Button
        binding.btnConnect.setOnClickListener {
            if (rtmpCamera2?.isStreaming == true) {
                stopStream()
            } else {
                startStream()
            }
        }

        // Eco Mode Button
        binding.btnEcoMode.setOnClickListener {
            toggleEcoMode()
        }

        // Zoom Slider
        binding.zoomSlider.addOnChangeListener { _, value, _ ->
            // Pass Float directly
            rtmpCamera2?.setZoom(value) 
        }
    }

    private fun startStream() {
        if (rtmpCamera2?.prepareAudio() == true && rtmpCamera2?.prepareVideo() == true) {
             // Hardcoded URL for initial testing
            val bossIp = "192.168.43.1" // Common hotspot gateway
            val streamUrl = "rtmp://$bossIp:1935/live/camera1" 
            
            try {
                rtmpCamera2?.startStream(streamUrl)
                binding.btnConnect.text = getString(R.string.btn_stop)
                binding.statusText.text = "LIVE: $streamUrl"
                binding.tallyBorder.visibility = View.VISIBLE
            } catch (e: Exception) {
                 showToast("Error starting stream: ${e.message}")
            }
        } else {
            showToast("Error preparing stream, check resolution/bitrate support")
        }
    }

    private fun stopStream() {
        rtmpCamera2?.stopStream()
        binding.btnConnect.text = getString(R.string.btn_connect)
        binding.statusText.text = getString(R.string.status_disconnected)
        binding.tallyBorder.visibility = View.GONE
    }
    
    // Toggle Eco Mode (Black Screen Overlay)
    private var isEcoMode = false
    private fun toggleEcoMode() {
        isEcoMode = !isEcoMode
        
        val layout = window.attributes
        layout.screenBrightness = if (isEcoMode) 0.1f else -1f // -1 is preferred
        window.attributes = layout
        
        binding.btnEcoMode.text = if (isEcoMode) "Normal Mode" else getString(R.string.mode_eco)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (rtmpCamera2?.isStreaming == true) {
            rtmpCamera2?.stopStream()
        }
        if (rtmpCamera2?.isOnPreview == true) {
            rtmpCamera2?.stopPreview()
        }
    }

    // ConnectChecker Implementation
    override fun onConnectionSuccess() {
        runOnUiThread {
            showToast("Connection Success")
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            showToast("Connection Failed: $reason")
            stopStream()
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        // Optional: Update UI with bitrate
    }

    override fun onDisconnect() {
        runOnUiThread {
            showToast("Disconnected")
            stopStream()
        }
    }

    override fun onAuthError() {
         runOnUiThread {
            showToast("Auth Error")
            stopStream()
        }
    }
    
    override fun onAuthSuccess() {
         runOnUiThread {
            showToast("Auth Success")
        }
    }

    override fun onConnectionStarted(url: String) {
        // Optional: Show status
    }
}
