/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.boss

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource

/**
 * Manages ExoPlayer instances for playing camera feeds.
 * 
 * Supports RTSP streams from connected cameras.
 */
@OptIn(UnstableApi::class)
class CameraFeedPlayer(private val context: Context) {

    companion object {
        private const val TAG = "CameraFeedPlayer"
    }

    data class CameraSlot(
        var player: ExoPlayer? = null,
        var url: String? = null,
        var isActive: Boolean = false
    )

    private val cameraSlots = arrayOf(
        CameraSlot(),
        CameraSlot(),
        CameraSlot(),
        CameraSlot()
    )

    /**
     * Connect a camera feed to a specific slot (0-3).
     * 
     * @param slot Slot index (0-3)
     * @param rtspUrl RTSP URL of the camera feed
     * @param surfaceView SurfaceView to render the video
     */
    fun connectCamera(slot: Int, rtspUrl: String, surfaceView: SurfaceView) {
        if (slot !in 0..3) {
            Log.e(TAG, "Invalid slot: $slot")
            return
        }

        // Release existing player if any
        disconnectCamera(slot)

        val player = ExoPlayer.Builder(context).build().apply {
            // Create RTSP media source
            val mediaItem = MediaItem.fromUri(rtspUrl)
            val rtspMediaSource = RtspMediaSource.Factory()
                .createMediaSource(mediaItem)

            setMediaSource(rtspMediaSource)
            setVideoSurfaceView(surfaceView)
            
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error on slot $slot: ${error.message}")
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            Log.d(TAG, "Slot $slot is ready")
                        }
                        Player.STATE_BUFFERING -> {
                            Log.d(TAG, "Slot $slot is buffering")
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Slot $slot playback ended")
                        }
                        Player.STATE_IDLE -> {
                            Log.d(TAG, "Slot $slot is idle")
                        }
                    }
                }
            })

            prepare()
            playWhenReady = true
        }

        cameraSlots[slot] = CameraSlot(player, rtspUrl, true)
        Log.d(TAG, "Connected camera $slot to $rtspUrl")
    }

    /**
     * Disconnect a camera from a slot.
     */
    fun disconnectCamera(slot: Int) {
        if (slot !in 0..3) return
        
        cameraSlots[slot].player?.apply {
            stop()
            release()
        }
        cameraSlots[slot] = CameraSlot()
        Log.d(TAG, "Disconnected camera $slot")
    }

    /**
     * Get the active camera URLs.
     */
    fun getActiveCameras(): List<Pair<Int, String>> {
        return cameraSlots.mapIndexedNotNull { index, slot ->
            if (slot.isActive && slot.url != null) {
                Pair(index, slot.url!!)
            } else {
                null
            }
        }
    }

    /**
     * Check if a slot has an active camera.
     */
    fun isSlotActive(slot: Int): Boolean {
        return slot in 0..3 && cameraSlots[slot].isActive
    }

    /**
     * Pause all players.
     */
    fun pauseAll() {
        cameraSlots.forEach { it.player?.pause() }
    }

    /**
     * Resume all players.
     */
    fun resumeAll() {
        cameraSlots.forEach { it.player?.play() }
    }

    /**
     * Release all players.
     */
    fun releaseAll() {
        cameraSlots.forEachIndexed { index, _ ->
            disconnectCamera(index)
        }
    }
}
