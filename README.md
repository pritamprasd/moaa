# MotherOfAllApps (MOAA) ⚡🔮

> **A futuristic, modular Android powerhouse featuring a Liquid Glass cyberpunk UI, high-performance FTP Server & Client, full-spectrum NFC Tool, centralized System Log Hub, and instant launcher shortcuts.**

---

[![Android](https://img.shields.io/badge/Platform-Android_14+_(API_34--37)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2026.09.00-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Theme](https://img.shields.io/badge/Theme-Liquid_Glass_Glassmorphic-00F5FF.svg)](#design--theme)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

---

## 🌟 Overview

**MotherOfAllApps** is a modern, modular Android application built from the ground up using **Jetpack Compose**, **Kotlin Coroutines/Flow**, and clean architectural patterns. It blends a state-of-the-art **Glassmorphic Liquid Glass** design identity with heavy-duty productivity tools for power users, developers, network engineers, and hardware enthusiasts.

---

## 🚀 Key Modules & Capabilities

### 1. 🔮 Liquid Glass Transparency Theme
- **Fluid Chromatic Mesh Background**: Ambient radiant light orbs (Cyan, Violet, Rose) glowing beneath frosted translucent acrylic layers.
- **Glassmorphic Isometric Surfaces**: Dual-layer neon refraction glow, specular light highlight borders, and fluid transparency across cards, modals, dropdowns, and top app bars.
- **Silky 120 FPS Rendering**: Optimized composable rendering with zero GPU overhead or lagging blur shaders.

### 2. 📡 FTP Server (Wi-Fi File Sharing)
- **High-Throughput Concurrent Engine**: Custom RFC-compliant async FTP server powered by Kotlin Coroutines (`Dispatchers.IO`).
- **Foreground Service**: Reliable background operation with active notification controls and persistent wake locks.
- **Active Client Telemetry**: Live connected client monitor, throughput telemetry, and authenticated session management.
- **Sandboxed Root Directory**: Configurable shared root folder (Downloads, Pictures, App External Storage) with chroot normalization.

### 3. 📁 FTP Client (Remote Server Mount & Sync)
- **Full Client Protocol Support**: Supports `PASSIVE` & `PORT` data modes, `MLSD`/`LIST` parsing, `RETR` downloads, `STOR` uploads, `DELE`, `MKD`, and `RMD`.
- **Saved Connection Profiles**: Securely stores FTP server bookmarks (host, port, credentials, active/passive mode).
- **Background Transfer Engine**: Streamlined file queue with real-time transfer progress and cancellation.

### 4. 🏷️ Full-Spectrum NFC Suite
- **Advanced Multi-Tech Tag Reader**: Decodes NDEF records, Mifare Classic / Ultralight, ISO 14443-4 (IsoDep), and FeliCa (NfcF).
- **NDEF Payload Studio**: Write Text, URLs, Wi-Fi network credentials, vCard contacts, SMS, and custom MIME payloads.
- **HCE Tag Emulation**: Emulate virtual NFC Type 4 smart cards directly from Android.
- **Tag Cloning & Raw APDU Console**: Read, duplicate, and execute direct ISO 7816-4 APDU command transcripts.

### 5. 🤖 Local LLM Gateway (Loopback Proxy & Failover Engine)
- **Local Loopback HTTP Server**: Embedded proxy running on `http://127.0.0.1:8080` exposing an OpenAI-compatible spec (`POST /v1/chat/completions`, `GET /v1/gateway/status`, `GET /v1/gateway/profiles`, `GET /v1/models`) supporting both JSON and real-time SSE chunked streaming.
- **Hybrid Multi-Account & LAN Pool**: Mix cloud accounts (Google Gemini Pro/Flash via OAuth/API Key, OpenAI ChatGPT via OAuth/Key) and desktop local hosts (Ollama on `11434`, LM Studio on `1234`, vLLM on `8000`) in the same unified routing pool.
- **Intelligent Automatic Failover**: Zero-downtime failover sequence re-routing active requests if a cloud account hits rate limits (`HTTP 429`) or if a desktop server disconnects (`ConnectException`).
- **LAN Wi-Fi Auto-Discovery**: Subnet sweeping scanner automatically discovering active Ollama & LM Studio instances running on local Wi-Fi with one-tap addition.
- **Settings Tab Controls**: Toggle gateway server on/off directly from App Settings.

### 6. 🎛️ Sensors Live (Dynamic Sampling Monitor)
- **Universal Hardware Sensor Discovery**: Discovers all onboard physical hardware sensors (Accelerometer, Gyroscope, Magnetometer, Barometer, Light, Proximity, Temperature, Relative Humidity, Step Counter, Orientation, etc.).
- **Dynamic Live Sampling Rates**: Instant runtime switching between `1s`, `2s`, `5s`, `Live (Fast)`, or `Paused` sampling intervals with throttled event emitters.
- **Rich Telemetry Visualizers**: Real-time meters, 3-axis vectors, physical units (`m/s²`, `rad/s`, `µT`, `lx`, `hPa`, `°C`), accuracy ratings, and expandable hardware specifications.
- **Snapshot Export**: Copy complete telemetry snapshots of all sensors and live values to the clipboard.

### 7. 📜 Centralized System Log Hub
- **Universal Log Aggregator**: All built-in and future tools stream real-time debug telemetry to `AppLogHub`.
- **Multiselect Dropdown Filters**: Filter by log levels (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) and tools.
- **Highlighting & Unknown Data Detection**: Automatic color-coded badging for File Ops, NFC Ops, Clipboard Ops, and raw hex payload alerts.
- **Memory Management**: Configurable auto-delete retention policies (1 Hour, 1 Day, 1 Week, 1 Month, 1 Year, All).

### 8. ⚙️ Dynamic Dashboard & Launcher Shortcuts
- **Configurable Gallery Layout**: Switch seamlessly between 1-Column, 2-Column, 3-Column, or 4-Column grids.
- **Interactive Morphing Search**: Animated search bar expanding fluidly beneath the header with real-time category filtering.
- **Pin to Homescreen**: Create dedicated launcher shortcuts for any tool, opening the tool directly and bypassing the dashboard.

---

## 🛠️ Architecture & Tech Stack

```
mother_of_all_apps/
├── app/                  # Android Host Application (Compose UI, Services, Navigation)
│   ├── src/main/java/dev/motherofallapps/host/
│   │   ├── config/       # Tool registry static configurations
│   │   ├── ftp/          # FTP Server engine, service, and UI
│   │   ├── host/         # Application container and state management
│   │   ├── logging/      # Universal AppLogHub and retention pruner
│   │   ├── settings/     # App settings manager and UI
│   │   ├── shortcut/     # Pin shortcut manager for Android launcher
│   │   ├── tool/         # Modular tools: LLM Gateway, FTP Client, NFC Suite, Sensors Live, Log Viewer
│   │   ├── ui/           # Theme, Navigation, Dashboard, and Glass Components
│   └── src/test/         # Comprehensive Unit Tests (LLM Gateway, FTP, NFC, Logs, Sensors)
│   │   ├── ui/           # Theme, Navigation, Dashboard, and Glass Components
│   └── src/test/         # Comprehensive Unit Tests (FTP, NFC, Logs, Profiles)
└── plugin-api/           # Lightweight contract for modular tool plugins
```

- **Language**: Kotlin 2.4.20
- **UI Framework**: Jetpack Compose + Material 3
- **Async Runtime**: Kotlin Coroutines & Reactive `StateFlow`
- **Minimum SDK**: Android 14 (API 34)
- **Target SDK**: Android 15 / 16 (API 36 / 37)
- **Zero Third-Party Bloat**: Built cleanly on native Android APIs and pure Kotlin libraries.

---

## 🏗️ Getting Started & Building

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+ or JDK 21
- Android SDK 34+

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/your-username/mother_of_all_apps.git
cd mother_of_all_apps

# Build Debug APK & Run Unit Tests
./gradlew assembleDebug test
```

The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Security & Privacy Notice

- **No Remote Telemetry**: Zero external analytics, tracking, or advertising SDKs.
- **Local Logs**: All system diagnostic logs reside purely in volatile memory or local sandbox preferences.
- **Local Credentials**: FTP profiles and connection parameters remain strictly on device.

---

## 📄 License

```
Copyright 2026 MotherOfAllApps Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
