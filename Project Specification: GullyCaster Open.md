Project Specification: GullyCaster Open Broadcast Suite
Version: 1.0.0-draft License: GNU General Public License v3.0 (GPLv3) Target Platform: Android (Min SDK 26 / Android 8.0) Languages: Kotlin (Primary), C++ (Native Libs) Localization: English, Urdu (اردو), Hindi (हिंदी), Bangla (বাংলা)

1. System Architecture Overview
The system operates on a Star Topology created via the Android SoftAP (Software Access Point/Hotspot) interface.

Node A (The Umpire): The Master Device. It enables a Wi-Fi Hotspot, runs an internal RTMP/SRT server, runs the Scoreboard web server, mixes the video feeds, and uplinks to YouTube/Facebook via 4G/5G.

Node B...N (The Bowlers): Client Devices. They connect to The Umpire's Wi-Fi hotspot and push camera feeds via RTMP/SRT to The Umpire's local IP (typically 192.168.43.1).

2. Application Specifications
We will develop two distinct Android applications (or two modes within one app):

App 1: GullyCam (The Client)

A lightweight camera encoder optimized for thermal efficiency and background streaming.

Core Features:

Protocol: RTMP (Real-Time Messaging Protocol) & SRT (Secure Reliable Transport).

Library: com.github.pedroSG94.RootEncoder

Connection Logic:

Auto-discovery: Scans for the "GullyCaster" hotspot gateway (usually 192.168.43.1).

Retry Logic: Aggressive reconnection policy (1s interval) if Wi-Fi fluctuates.

Camera Control:

Zoom Slider (0.5x to 10x).

ISO/Exposure manual locks (critical for bright cricket fields).

Eco-Mode (Black Screen): A button to turn the OLED screen completely black (rendering a floating "Stop" icon only) to prevent overheating while streaming.

Tally Light: Visual border turns Red when the Master is "Live" with this camera (requires bidirectional socket comms).

App 2: GullyDirector (The Master)

The central hub. Requires a high-end device (Snapdragon 8 Gen 1+ recommended).

Core Features:

Network Manager:

Triggers startLocalOnlyHotspot() or prompts user to enable System Tethering.

Displays current SSID and Password for clients via large QR Code.

Ingest Server (The Localhost):

Option A (Embedded Nginx): Bundles a cross-compiled libnginx-mod-rtmp.so to run a local RTMP server on port 1935.

Option B (Java Implementation): Uses a Kotlin-based RTMP server socket listener to accept incoming streams.

Video Mixer (The Switcher):

Multi-View: 2x2 Grid showing connected Client feeds.

Preview/Program: Tap a feed to send it to "Program" (Live Output).

Picture-in-Picture: Ability to overlay the Scoreboard (WebView) on the video.

Scoring Engine:

Runs a local HTTP Server (NanoHTTPD on port 8080).

Hosts a local HTML/JS/CSS scoreboard page supporting Nastaliq (Urdu) and Devanagari (Hindi/Bangla) fonts.

UI Inputs: Buttons for "4", "6", "Out", "Wide". Updates score.json locally which the Overlay reads.

Uplink Bridge:

Binds the output stream socket specifically to the Cellular Network Interface (ConnectivityManager.TYPE_MOBILE) while keeping the Input server bound to the Wi-Fi Interface. This allows simultaneous Hotspot usage + Internet Upload.

3. UI/UX & Localization Strategy
The UI must support Right-to-Left (RTL) layouts natively for Urdu.

Language Resource Files (res/values/strings.xml):

Key ID	English	Urdu (ur)	Hindi (hi)	Bangla (bn)
app_name	GullyCaster	گلی کاسٹر	गलीकास्टर	গলি কাস্টার
btn_connect	Connect to Umpire	امپائر سے جڑیں	अंपायर से जुड़ें	আম্পায়ারের সাথে সংযোগ করুন
status_live	LIVE	براہ راست	लाइव	সরাসরি
score_runs	Runs	رنز	रन	রান
mode_eco	Battery Saver	بیٹری سیور	बैटरी सेवर	ব্যাটারি সেভার
Font Assets:

Urdu: Jameel Noori Nastaleeq (Must be embedded, as Android default Urdu font is Naskh and looks plain).

Hindi/Bangla: Google Noto Sans.

4. Open Source Governance Rules
To maintain the project as a healthy open-source initiative, the following rules apply:

4.1 Contribution Guidelines (CONTRIBUTING.md)

Branching Model: Use "Gitflow".

main: Stable release code.

develop: Active development.

feature/feature-name: For new additions (e.g., feature/scoreboard-update).

Code Style:

Kotlin code must follow the ktlint standard.

Comments must be in English for universal maintainability, but UI strings must be localized.

Pull Request (PR) Policy:

All PRs must include a screen recording if UI changes are made.

No hardcoded API keys (e.g., YouTube Stream Keys) in the code.

4.2 Code of Conduct

Prioritize support for low-end devices. Do not merge code that unnecessarily increases RAM usage (e.g., heavy animation libraries) as the target market uses mid-tier Androids.

Respect regional diversity. All feature discussions must consider how they impact Urdu/Hindi/Bangla usability.

4.3 License Header

Every source file must include:

Kotlin
/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/your-repo/gullycaster
 * Developed for the Cricket Community of South Asia.
 */
5. Technical Implementation Steps (Phase 1)
Scaffold the Project: Create an Android Studio project with two modules (:camera and :director).

Implement RootEncoder: Integrate com.github.pedroSG94.RootEncoder in the :camera module to test basic RTMP pushing.

Build the SoftAP Logic: In the :director module, use WifiManager.startLocalOnlyHotspot (API 26+) to create the local network programmatically.

Network Binding: Write the logic in :director to force the RTMP Publisher to use the Cellular Network NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).

Scoreboard Web Server: Implement NanoHTTPD to serve a static HTML file from the assets folder to http://localhost:8080/score.html.

6. Hardware Recommendations for Users
Master Device: Samsung S20/S21/S22 series (Must support "Separate App Sound" or good thermal handling) or Pixel 6/7.

Camera Devices: Any Android phone with Android 8.0+.

Cooling: Recommended use of magnetic phone coolers (Peltier coolers) for the Master device, as Hotspot + Encoding + 5G Uplink generates significant heat.