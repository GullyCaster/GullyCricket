/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.boss

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gullycaster.boss.databinding.ActivityMainBinding
import com.gullycaster.common.Constants

/**
 * GullyBoss - Main director/control room activity.
 * 
 * This activity provides:
 * - Hotspot creation and management
 * - Multi-view video grid from connected cameras
 * - Video switching (Preview/Program)
 * - Scoreboard controls
 * - Uplink to YouTube/Facebook
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    // Core components
    private lateinit var hotspotManager: HotspotManager
    private lateinit var scoreboardServer: ScoreboardServer
    private lateinit var nsdAdvertiser: NsdAdvertiser
    private lateinit var cameraFeedPlayer: CameraFeedPlayer
    private lateinit var cameraDiscovery: CameraDiscovery
    
    // Discovered cameras queue
    private val pendingCameras = mutableListOf<Pair<String, String>>()
    
    // Camera surface views
    private val cameraSurfaces: Array<SurfaceView> by lazy {
        arrayOf(
            binding.camera1,
            binding.camera2,
            binding.camera3,
            binding.camera4
        )
    }
    
    // Selected camera for switching
    private var selectedCamera = 0
    
    // State
    private var isHotspotActive = false
    private var runs = 0
    private var wickets = 0
    private var overs = 0.0
    
    // Undo history
    data class ScoreAction(val runsChange: Int, val wicketsChange: Int, val oversChange: Double)
    private val history = mutableListOf<ScoreAction>()

    // Permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startHotspot()
        } else {
            showToast("Location permission required for hotspot")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        setupUI()
        setupCameraGrid()
        startScoreboardServer()
    }

    private fun initComponents() {
        hotspotManager = HotspotManager(this)
        scoreboardServer = ScoreboardServer(Constants.HTTP_PORT)
        nsdAdvertiser = NsdAdvertiser(this)
        cameraFeedPlayer = CameraFeedPlayer(this)
        cameraDiscovery = CameraDiscovery(this)
    }

    private fun setupUI() {
        // Hotspot button
        binding.btnHotspot.setOnClickListener {
            if (isHotspotActive) {
                stopHotspot()
            } else {
                requestHotspotPermissions()
            }
        }
        
        // Score buttons
        binding.btn1.setOnClickListener { addRuns(1) }
        binding.btn2.setOnClickListener { addRuns(2) }
        binding.btn3.setOnClickListener { addRuns(3) }
        binding.btn4.setOnClickListener { addRuns(4) }
        binding.btn6.setOnClickListener { addRuns(6) }
        binding.btnWide.setOnClickListener { addExtra(1, isNoBallDelivery = false) }
        binding.btnNoBall.setOnClickListener { addExtra(1, isNoBallDelivery = true) }
        binding.btnOut.setOnClickListener { addWicket() }
        binding.btnUndo.setOnClickListener { undoLastAction() }
        
        // Refresh Cameras button
        binding.btnRefreshCameras.setOnClickListener {
            startCameraDiscovery()
        }
        
        // Go Live button
        binding.btnGoLive.setOnClickListener {
            showToast("RTMP uplink not yet implemented")
        }
        
        updateScoreDisplay()
        
        // Auto-start discovery if on a network
        startCameraDiscovery()
    }

    private fun setupCameraGrid() {
        // Set click listeners on camera views for selection
        cameraSurfaces.forEachIndexed { index, surfaceView ->
            surfaceView.setOnClickListener {
                selectCamera(index)
            }
            
            // Long press to connect a test RTSP stream
            surfaceView.setOnLongClickListener {
                showCameraConnectDialog(index)
                true
            }
        }
    }

    private fun selectCamera(index: Int) {
        // Update selection state
        cameraSurfaces.forEachIndexed { i, view ->
            // Visual feedback - selected camera gets highlighted border
            view.alpha = if (i == index) 1.0f else 0.7f
        }
        selectedCamera = index
        showToast("Camera ${index + 1} selected")
    }

    private fun showCameraConnectDialog(slot: Int) {
        // Check if we have pending discovered cameras
        if (pendingCameras.isNotEmpty()) {
            val (name, url) = pendingCameras.removeAt(0)
            cameraFeedPlayer.connectCamera(slot, url, cameraSurfaces[slot])
            showToast("Connecting $name to slot ${slot + 1}...")
        } else if (cameraFeedPlayer.isSlotActive(slot)) {
            cameraFeedPlayer.disconnectCamera(slot)
            showToast("Camera ${slot + 1} disconnected")
        } else {
            showToast("No cameras discovered. Start hotspot first.")
        }
    }

    /**
     * Connect a camera to a specific slot.
     * Called when a camera is discovered via NSD.
     */
    fun connectCameraFeed(slot: Int, rtspUrl: String) {
        if (slot in 0..3) {
            cameraFeedPlayer.connectCamera(slot, rtspUrl, cameraSurfaces[slot])
        }
    }

    private fun startScoreboardServer() {
        if (scoreboardServer.startServer()) {
            showToast("Scoreboard server started on port ${Constants.HTTP_PORT}")
        } else {
            showToast("Failed to start scoreboard server")
        }
    }

    private fun requestHotspotPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            startHotspot()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun startHotspot() {
        hotspotManager.startHotspot(object : HotspotManager.HotspotCallback {
            override fun onHotspotStarted(ssid: String, password: String) {
                runOnUiThread {
                    isHotspotActive = true
                    binding.btnHotspot.text = getString(R.string.btn_stop_hotspot)
                    binding.hotspotStatus.text = "SSID: $ssid\nPass: $password"
                    
                    // Register NSD service so cameras can discover us
                    nsdAdvertiser.registerService(Constants.RTMP_PORT)
                    
                    // Start discovering cameras
                    startCameraDiscovery()
                    
                    showToast("Hotspot active! Finding cameras...")
                }
            }

            override fun onHotspotStopped() {
                runOnUiThread {
                    isHotspotActive = false
                    binding.btnHotspot.text = getString(R.string.btn_start_hotspot)
                    binding.hotspotStatus.text = getString(R.string.status_disconnected)
                }
            }

            override fun onHotspotFailed(reason: Int) {
                runOnUiThread {
                    showToast("LocalOnlyHotspot failed. Opening system tethering settings...")
                    openTetheringSettings()
                }
            }
        })
    }

    private fun stopHotspot() {
        cameraDiscovery.stopDiscovery()
        nsdAdvertiser.unregisterService()
        hotspotManager.stopHotspot()
        isHotspotActive = false
        pendingCameras.clear()
        binding.btnHotspot.text = getString(R.string.btn_start_hotspot)
        binding.hotspotStatus.text = getString(R.string.status_disconnected)
    }

    private fun startCameraDiscovery() {
        cameraDiscovery.startDiscovery(object : CameraDiscovery.DiscoveryCallback {
            override fun onDiscoveryStarted() {
                runOnUiThread {
                    showToast("Searching for cameras...")
                }
            }

            override fun onCameraFound(name: String, host: java.net.InetAddress, port: Int) {
                runOnUiThread {
                    val rtspUrl = "rtsp://${host.hostAddress}:$port/live"
                    pendingCameras.add(Pair(name, rtspUrl))
                    showToast("Camera found: $name")
                    
                    // Auto-connect to first available slot
                    for (i in 0..3) {
                        if (!cameraFeedPlayer.isSlotActive(i)) {
                            val (camName, url) = pendingCameras.removeAt(0)
                            cameraFeedPlayer.connectCamera(i, url, cameraSurfaces[i])
                            showToast("Auto-connected to $camName")
                            break
                        }
                    }
                }
            }

            override fun onCameraLost(name: String) {
                runOnUiThread {
                    pendingCameras.removeAll { it.first == name }
                    showToast("Camera offline: $name")
                }
            }

            override fun onDiscoveryError(errorCode: Int) {
                runOnUiThread {
                    showToast("Discovery error: $errorCode")
                }
            }
        })
    }

    private fun addRuns(value: Int) {
        val prevOvers = overs
        runs += value
        scoreboardServer.addRuns(value)
        scoreboardServer.addBall()
        updateOvers()
        history.add(ScoreAction(value, 0, overs - prevOvers))
        updateScoreDisplay()
    }

    private fun addWicket() {
        if (wickets < 10) {
            val prevOvers = overs
            wickets++
            scoreboardServer.addWicket()
            scoreboardServer.addBall()
            updateOvers()
            history.add(ScoreAction(0, 1, overs - prevOvers))
            updateScoreDisplay()
        }
    }

    /**
     * Add extra runs (wide or no ball).
     * Wide: 1 run, no ball counted
     * No Ball: 1 run + free hit, no ball counted
     */
    private fun addExtra(extraRuns: Int, isNoBallDelivery: Boolean) {
        runs += extraRuns
        scoreboardServer.addRuns(extraRuns)
        // No ball is not counted in overs
        history.add(ScoreAction(extraRuns, 0, 0.0))
        updateScoreDisplay()
        if (isNoBallDelivery) {
            showToast("No Ball - Free Hit!")
        }
    }

    /**
     * Undo the last score action.
     */
    private fun undoLastAction() {
        if (history.isEmpty()) {
            showToast("Nothing to undo")
            return
        }
        
        val lastAction = history.removeAt(history.size - 1)
        
        // Reverse the action
        runs = maxOf(0, runs - lastAction.runsChange)
        wickets = maxOf(0, wickets - lastAction.wicketsChange)
        overs = maxOf(0.0, overs - lastAction.oversChange)
        
        // Update scoreboard server (reset and replay would be ideal, but we'll sync state)
        scoreboardServer.updateScore(runs = runs, wickets = wickets, overs = overs)
        
        updateScoreDisplay()
    }

    /**
     * Open system tethering/hotspot settings as fallback.
     */
    private fun openTetheringSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to general settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                showToast("Please enable hotspot manually in Settings")
            }
        }
    }

    private fun updateOvers() {
        val balls = ((overs * 10) % 10).toInt()
        if (balls >= 5) {
            overs = (overs.toInt() + 1).toDouble()
        } else {
            overs += 0.1
        }
    }

    private fun updateScoreDisplay() {
        binding.scoreText.text = "$runs/$wickets"
        binding.oversText.text = String.format("%.1f Overs", overs)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        cameraFeedPlayer.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        cameraFeedPlayer.resumeAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDiscovery.stopDiscovery()
        cameraFeedPlayer.releaseAll()
        nsdAdvertiser.unregisterService()
        hotspotManager.stopHotspot()
        scoreboardServer.stopServer()
    }
}
