/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

package com.gullycaster.boss

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.IOException

/**
 * HTTP Server for the Cricket Scoreboard.
 * 
 * Serves:
 * - /score.json  - Current score data as JSON
 * - /score.html  - Scoreboard overlay page (for OBS/streaming)
 * - /api/update  - POST endpoint to update score
 */
class ScoreboardServer(port: Int = 8080) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "ScoreboardServer"
    }

    // Score state
    private var runs = 0
    private var wickets = 0
    private var overs = 0.0
    private var team1Name = "Team A"
    private var team2Name = "Team B"
    private var battingTeam = 1

    /**
     * Update the score.
     */
    fun updateScore(
        runs: Int? = null,
        wickets: Int? = null,
        overs: Double? = null,
        team1: String? = null,
        team2: String? = null
    ) {
        runs?.let { this.runs = it }
        wickets?.let { this.wickets = it }
        overs?.let { this.overs = it }
        team1?.let { this.team1Name = it }
        team2?.let { this.team2Name = it }
    }

    /**
     * Add runs to the score.
     */
    fun addRuns(value: Int) {
        runs += value
    }

    /**
     * Add a wicket.
     */
    fun addWicket() {
        if (wickets < 10) wickets++
    }

    /**
     * Increment overs by 0.1 (1 ball).
     */
    fun addBall() {
        val balls = ((overs * 10) % 10).toInt()
        if (balls >= 5) {
            overs = (overs.toInt() + 1).toDouble()
        } else {
            overs += 0.1
        }
    }

    /**
     * Get current score as JSON.
     */
    fun getScoreJson(): JSONObject {
        return JSONObject().apply {
            put("runs", runs)
            put("wickets", wickets)
            put("overs", String.format("%.1f", overs))
            put("team1", team1Name)
            put("team2", team2Name)
            put("battingTeam", battingTeam)
            put("display", "$runs/$wickets (${"%.1f".format(overs)} ov)")
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "Request: $method $uri")

        return when {
            uri == "/score.json" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    getScoreJson().toString()
                )
            }

            uri == "/score.html" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    getScoreboardHtml()
                )
            }

            uri == "/api/update" && method == Method.POST -> {
                try {
                    val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                    val body = ByteArray(contentLength)
                    session.inputStream.read(body)
                    val json = JSONObject(String(body))
                    
                    json.optInt("runs", -1).takeIf { it >= 0 }?.let { runs = it }
                    json.optInt("wickets", -1).takeIf { it >= 0 }?.let { wickets = it }
                    json.optDouble("overs", -1.0).takeIf { it >= 0 }?.let { overs = it }
                    json.optString("team1").takeIf { it.isNotEmpty() }?.let { team1Name = it }
                    json.optString("team2").takeIf { it.isNotEmpty() }?.let { team2Name = it }

                    newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        """{"status":"ok"}"""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating score", e)
                    newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        """{"error":"${e.message}"}"""
                    )
                }
            }

            uri == "/api/add" && method == Method.POST -> {
                try {
                    val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                    val body = ByteArray(contentLength)
                    session.inputStream.read(body)
                    val json = JSONObject(String(body))
                    
                    json.optInt("runs", 0).takeIf { it > 0 }?.let { addRuns(it) }
                    if (json.optBoolean("wicket", false)) addWicket()
                    if (json.optBoolean("ball", false)) addBall()

                    newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        getScoreJson().toString()
                    )
                } catch (e: Exception) {
                    newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        """{"error":"${e.message}"}"""
                    )
                }
            }

            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "Not Found"
                )
            }
        }
    }

    private fun getScoreboardHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GullyCaster Scoreboard</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@700&display=swap');
        
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: 'Outfit', sans-serif;
            background: transparent;
        }
        
        .scoreboard {
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            border: 2px solid #e94560;
            border-radius: 12px;
            padding: 16px 24px;
            display: inline-block;
            box-shadow: 0 4px 20px rgba(233, 69, 96, 0.3);
        }
        
        .team-name {
            color: #e94560;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 2px;
            margin-bottom: 4px;
        }
        
        .score {
            color: #fff;
            font-size: 48px;
            font-weight: 700;
            line-height: 1;
        }
        
        .overs {
            color: #a3a3a3;
            font-size: 18px;
            margin-top: 4px;
        }
    </style>
</head>
<body>
    <div class="scoreboard">
        <div class="team-name" id="team"></div>
        <div class="score"><span id="runs">0</span>/<span id="wickets">0</span></div>
        <div class="overs">(<span id="overs">0.0</span> ov)</div>
    </div>
    
    <script>
        async function updateScore() {
            try {
                const res = await fetch('/score.json');
                const data = await res.json();
                document.getElementById('runs').textContent = data.runs;
                document.getElementById('wickets').textContent = data.wickets;
                document.getElementById('overs').textContent = data.overs;
                document.getElementById('team').textContent = data.battingTeam === 1 ? data.team1 : data.team2;
            } catch (e) {
                console.error('Failed to fetch score:', e);
            }
        }
        
        updateScore();
        setInterval(updateScore, 1000);
    </script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Start the server safely.
     */
    fun startServer(): Boolean {
        return try {
            start(SOCKET_READ_TIMEOUT, false)
            Log.d(TAG, "Scoreboard server started on port $listeningPort")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start server", e)
            false
        }
    }

    /**
     * Stop the server safely.
     */
    fun stopServer() {
        stop()
        Log.d(TAG, "Scoreboard server stopped")
    }
}
