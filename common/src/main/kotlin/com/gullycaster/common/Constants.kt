/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.common

/**
 * Common constants shared across GullyCam and GullyBoss.
 */
object Constants {
    // Default RTMP port
    const val RTMP_PORT = 1935
    
    // Default SRT port
    const val SRT_PORT = 9000
    
    // Scoreboard HTTP server port
    const val HTTP_PORT = 8080
    
    // Default hotspot gateway IP
    const val HOTSPOT_GATEWAY = "192.168.43.1"
    
    // Hotspot SSID prefix
    const val HOTSPOT_SSID_PREFIX = "GullyCaster"
    
    // Retry intervals
    const val RECONNECT_INTERVAL_MS = 1000L
    const val MAX_RECONNECT_ATTEMPTS = 30
}
