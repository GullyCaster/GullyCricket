# GullyCaster Open Broadcast Suite

🏏 **Live-stream cricket matches from your backyard with just Android phones!**

An open-source broadcast system designed for the cricket-loving communities of South Asia. GullyCaster turns a group of Android phones into a professional multi-camera streaming setup—no expensive equipment needed.

## 🎯 What is GullyCaster?

GullyCaster consists of two Android apps:

| App | Role | Description |
|-----|------|-------------|
| **GullyCam** | 📹 Camera | Lightweight camera encoder that streams to the master device |
| **GullyBoss** | 🎬 Director | Central hub that mixes feeds, controls the scoreboard, and uplinks to YouTube/Facebook |

### Architecture

```
┌─────────────────┐     Wi-Fi Hotspot      ┌─────────────────┐
│    GullyCam 1   │ ────────────────────── │                 │
│   (Bowler End)  │   RTMP/SRT Stream      │                 │
└─────────────────┘                        │                 │
                                           │   GullyBoss     │ ──── 4G/5G ──── YouTube/Facebook
┌─────────────────┐                        │   (The Umpire)  │
│    GullyCam 2   │ ────────────────────── │                 │
│  (Batsman End)  │   RTMP/SRT Stream      │                 │
└─────────────────┘                        └─────────────────┘
```

## ✨ Features

- **Multi-Camera Support** - Connect up to 4 phones as camera sources
- **Video Switching** - Switch between camera angles with a tap
- **Live Scoreboard** - Cricket scoring with overlay support
- **Eco Mode** - Black screen mode to reduce heat during streaming
- **Tally Lights** - Red border on camera when LIVE
- **4 Languages** - English, اردو (Urdu), हिंदी (Hindi), বাংলা (Bangla)

## 📱 Requirements

### GullyBoss (Director Device)
- Android 8.0+ (API 26)
- Recommended: Snapdragon 8 Gen 1+ or equivalent
- Good thermal handling (Samsung S20+, Pixel 6+)

### GullyCam (Camera Devices)  
- Android 8.0+ (API 26)
- Any phone with a decent camera

## 🚀 Getting Started

### Build from Source

```bash
# Clone the repository
git clone https://github.com/gullycaster/gullycaster.git
cd gullycaster

# Build both apps
./gradlew assembleDebug

# Install GullyBoss on master device
adb install boss/build/outputs/apk/debug/boss-debug.apk

# Install GullyCam on camera devices
adb install camera/build/outputs/apk/debug/camera-debug.apk
```

### Quick Setup

1. **Start GullyBoss** on your most powerful phone
2. Tap "Start Hotspot" to create the network
3. **Connect camera phones** to the GullyCaster Wi-Fi
4. **Open GullyCam** on camera phones → Tap "Connect to Umpire"
5. Configure your YouTube/Facebook stream key in GullyBoss
6. Tap **GO LIVE** 🔴

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Streaming**: [RootEncoder](https://github.com/pedroSG94/RootEncoder) (RTMP/SRT)
- **Web Server**: NanoHTTPD (for scoreboard)
- **Min SDK**: 26 (Android 8.0)
- **Build**: Gradle 8.9 + AGP 8.7.2

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Key Principles
- **Prioritize low-end devices** - Don't bloat memory usage
- **Respect regional diversity** - Consider Urdu/Hindi/Bangla usability
- **Follow ktlint** - Run `./gradlew ktlintCheck` before PRs

## 📄 License

```
Copyright (C) 2025 GullyCaster Project
Licensed under GNU General Public License v3.0 (GPLv3)
Developed for the Cricket Community of South Asia.
```

See [LICENSE](LICENSE) for the full license text.

---

**Made with ❤️ for gully cricket enthusiasts everywhere, by [Ikram Rasheed](https://www.ikramrasheed.com?utm=gullyCricket) and Hamza Mehboob** 🏏
