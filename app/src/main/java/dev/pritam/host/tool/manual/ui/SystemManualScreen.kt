package dev.pritam.host.tool.manual.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SpaceBackground
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.TextTertiary
import dev.pritam.host.ui.theme.Violet

data class ManualSection(
    val id: String,
    val title: String,
    val category: String,
    val tagline: String,
    val iconEmoji: String,
    val accentColor: Color,
    val toolRouteId: String? = null,
    val paragraphs: List<String>,
    val keyFeatures: List<Pair<String, String>> = emptyList(),
    val quickStartSteps: List<String> = emptyList(),
    val codeSnippets: List<Pair<String, String>> = emptyList(), // Label to Code
    val tips: List<String> = emptyList()
)

object SystemManualData {

    val SECTIONS: List<ManualSection> = listOf(
        ManualSection(
            id = "overview",
            title = "MotherOfAllApps System Overview",
            category = "Architecture",
            tagline = "Modular Clean Architecture, offline-first execution & privacy.",
            iconEmoji = "🏛️",
            accentColor = Cyan,
            paragraphs = listOf(
                "MotherOfAllApps (MOAA) is an elite, high-performance modular Android utility suite engineered with Jetpack Compose, Kotlin Coroutines, and strict Clean Architecture principles for Android 14+ (API 34..37).",
                "The entire application operates with zero third-party trackers, zero external telemetry SDKs, and zero cloud lock-in. All local servers, database files, LLM routing tables, and custom tools remain strictly on your device."
            ),
            keyFeatures = listOf(
                "Modular Clean Architecture" to "Independent tool plugins communicating via unidirectional StateFlow and SharedFlow buses.",
                "Embedded Local Loopback Hub" to "Integrated local HTTP servers for FTP (:2121) and LLM Gateway (:8080) on 127.0.0.1.",
                "Universal Diagnostics Bus" to "AppLogHub collects, indexes, and filters real-time operational telemetry across all tools.",
                "Zero Tracking & Local Sandboxing" to "No external analytics SDKs; all credentials and generated tools stay strictly local."
            ),
            tips = listOf(
                "Tip: You can pin any tool as a dedicated shortcut directly on your Android home screen from Settings > Homescreen Shortcuts or the dashboard card.",
                "Tip: Use the 3-tab navigation bar at the bottom to quickly jump between Dashboard, System Logs, and Settings."
            )
        ),
        ManualSection(
            id = "dynamic-tools",
            title = "AI Web App Generator",
            category = "AI & Dynamic Web Tools",
            tagline = "Synthesize, refine, edit, and run custom HTML/CSS/JS micro-apps on-device.",
            iconEmoji = "✨",
            accentColor = Color(0xFFF43F5E),
            toolRouteId = "dynamic-tools-studio",
            paragraphs = listOf(
                "The Dynamic Tools Studio allows you to create fully functional, interactive, lightweight runtime tools using simple natural language prompts connected to the local LLM Gateway.",
                "Generated tools are saved to the device's sandboxed storage (/data/user/0/<package>/files/custom_tools/<tool_id>/) containing manifest.json, index.html, styles.css, and app.js. They are dynamically registered into the main app dashboard gallery alongside native Kotlin tools."
            ),
            keyFeatures = listOf(
                "Prompt-Based Synthesis" to "Type a prompt or choose presets (Calculator, Unit Converter, Regex Lab, Pomodoro Timer, JSON Validator, Crypto Studio) to generate apps instantly.",
                "AI Prompt Refinement" to "Update existing generated tools by tapping '✨ Refine' and prompting the AI for iterative improvements without rewriting code manually.",
                "On-Device Code Editor" to "Inspect, tweak, and edit raw HTML, CSS, and JavaScript with instant hot-reloading in the sandboxed runner.",
                "Native AndroidBridge API" to "Access native hardware via window.AndroidBridge (showToast, vibrate, copyToClipboard, getDeviceInfo, log).",
                "JSON Bundle Export/Import" to "Share self-contained dynamic tool bundles with other devices via 1-click clipboard export and JSON import."
            ),
            quickStartSteps = listOf(
                "1. Open Dynamic Tools Studio from the Dashboard or tap '+ Create Tool'.",
                "2. Enter a prompt (e.g. 'Build an interactive Scientific Calculator with trig and log functions') or select a preset chip.",
                "3. Tap 'Synthesize & Launch Web App'. The LLM Gateway generates the HTML, CSS, JS, and manifest.",
                "4. Tap 'Launch' to open the app inside the sandboxed runner.",
                "5. Tap '✨ Refine' to prompt for new features, or '</> Code' to edit the code directly.",
                "6. The custom tool automatically appears in your main Dashboard grid with an 'AI WEB APP' badge."
            ),
            codeSnippets = listOf(
                "Calling Native Android Bridge from JS" to """
                    // Inside your dynamic tool's JavaScript:
                    if (window.AndroidBridge) {
                        window.AndroidBridge.copyToClipboard("Result: " + value);
                        window.AndroidBridge.vibrate(50); // 50ms tactile vibration
                        window.AndroidBridge.showToast("Copied to clipboard!");
                        window.AndroidBridge.log("User calculated value: " + value);
                    }
                """.trimIndent(),
                "Querying Device Information" to """
                    if (window.AndroidBridge) {
                        var info = JSON.parse(window.AndroidBridge.getDeviceInfo());
                        console.log("Device Model: " + info.model + ", Brand: " + info.brand);
                    }
                """.trimIndent()
            ),
            tips = listOf(
                "Tip: When prompting for refinements, be specific (e.g. 'Add a dark/light mode toggle and an operation history drawer').",
                "Tip: You can export any generated tool as a single JSON bundle to back it up or share it with others."
            )
        ),
        ManualSection(
            id = "nfc-tool",
            title = "NFC Tools Studio (3×3 Suite)",
            category = "Hardware & Contactless",
            tagline = "Complete contactless RFID/NFC diagnosis and NDEF payload studio.",
            iconEmoji = "📡",
            accentColor = Rose,
            toolRouteId = "nfc-tool",
            paragraphs = listOf(
                "NFC Tools Studio is an advanced RFID/NFC diagnostics and programming laboratory organized in a high-density 3×3 matrix grid.",
                "It supports the full ISO 14443-3A/4 and ISO 15693 protocol stacks, providing deep hardware inspection, NDEF payload writing, memory page dump analysis, and sector key decoding for NTAG, Mifare, and FeliCa silicon."
            ),
            keyFeatures = listOf(
                "1. Scan & Info" to "Hardware UID, RF protocols (NfcA, IsoDep, Ndef, MifareClassic), ATQA, SAK, and memory capacity inspection.",
                "2. Write Studio" to "Compose and write standard NDEF records: Text/Markdown, Web URLs, Wi-Fi Network Credentials (WPA2/WPA3), vCard Contacts, and App Launchers (AAR).",
                "3. Clone & Wipe" to "Clone scanned tag payloads, format/wipe tags to blank NDEF state, and apply permanent read-only lock protection.",
                "4. Silicon Pages" to "Fast-read raw hex memory page browser with ASCII mapping and continuous memory layout preview.",
                "5. Mifare Decode" to "Sector trailer and data block analyzer for Mifare Classic (1K/4K) using standard Key A/B keys.",
                "6. Chip Database" to "Reference datasheets and specs for NTAG213/215/216, Mifare Classic, Ultralight C, DESFire EV3, and Sony FeliCa.",
                "7. Amiibo / Game" to "Memory layout guides, page definitions, and key slots for Type-2 NTAG215 accessories.",
                "8. Apps Guide" to "Curated companion apps recommendations (NFC Tools, NXP TagWriter, NXP TagInfo).",
                "9. Tag Logs" to "Real-time diagnostic event stream with one-tap clipboard export."
            ),
            quickStartSteps = listOf(
                "1. Ensure NFC is enabled in Android System Settings.",
                "2. Open NFC Tools from the dashboard.",
                "3. To Scan: Tap 'Scan & Info' and hold any tag near the NFC antenna on the back of your device.",
                "4. To Write Wi-Fi: Tap 'Write Studio' > 'Wi-Fi Network', enter SSID and Password, tap 'Prepare Write', and tap the physical tag.",
                "5. To Inspect Memory: Tap 'Silicon Pages' and hold a tag to view the live hex memory map."
            ),
            tips = listOf(
                "Tip: NTAG213 has 144 bytes user memory, NTAG215 has 504 bytes, and NTAG216 has 888 bytes.",
                "Tip: Be cautious with the 'Permanent Lock' feature in Clone & Wipe as it permanently write-protects the physical tag."
            )
        ),
        ManualSection(
            id = "ftp-server",
            title = "LAN FTP Server",
            category = "Networking & File Transfer",
            tagline = "Host local device storage over Wi-Fi with custom credentials and ports.",
            iconEmoji = "📁",
            accentColor = Cyan,
            toolRouteId = "ftp-server",
            paragraphs = listOf(
                "The LAN FTP Server turns your Android phone into a high-concurrency Wi-Fi file server, allowing your PC, Mac, Linux workstation, or mobile FTP client to browse, upload, and download files at gigabit LAN speeds without cables."
            ),
            keyFeatures = listOf(
                "High Concurrency Engine" to "Asynchronous multi-client socket listener built on Kotlin Coroutines (Dispatchers.IO).",
                "RFC-Compliant Protocol" to "Supports USER, PASS, PORT, PASV, LIST, MLSD, RETR, STOR, DELE, MKD, RMD, RNFR, RNTO, and SIZE commands.",
                "Configurable Scopes" to "Choose root directories (Downloads, Documents, or Full Internal Storage) and custom ports (default :2121).",
                "Live Client Telemetry" to "Real-time connected IP counter, active transfer throughput meter, and one-tap client disconnects."
            ),
            quickStartSteps = listOf(
                "1. Connect your phone and computer to the same Wi-Fi network.",
                "2. Open LAN FTP Server and configure your Username, Password, and Port (default 2121).",
                "3. Tap 'Start FTP Server'.",
                "4. On Windows Explorer: Type 'ftp://<PHONE_IP>:2121' in the address bar.",
                "5. On macOS Finder: Press Cmd + K and enter 'ftp://<PHONE_IP>:2121'.",
                "6. On FileZilla: Host = <PHONE_IP>, Port = 2121, Protocol = FTP."
            ),
            tips = listOf(
                "Tip: For large file transfers, leave the FTP Server in the foreground or ensure battery optimization is disabled for MOAA so Android does not throttle background network sockets."
            )
        ),
        ManualSection(
            id = "ftp-client",
            title = "Remote FTP Client",
            category = "Networking & File Transfer",
            tagline = "Mount remote FTP servers, browse files & remember credentials.",
            iconEmoji = "🌐",
            accentColor = Emerald,
            toolRouteId = "ftp-client",
            paragraphs = listOf(
                "The Remote FTP Client lets you connect to remote FTP/FTPS servers across LAN or WAN, manage files, download remote logs or backups directly to phone storage, and queue file uploads."
            ),
            keyFeatures = listOf(
                "Connection Profiles" to "Securely bookmark server hostnames, ports, credentials, and Passive/Active mode preferences.",
                "Remote Explorer" to "Browse remote directories with breadcrumb path navigation, create folders, rename, and delete files.",
                "Transfer Queue" to "Asynchronous file download and upload engine with progress meters.",
                "System Logs Integration" to "All connection handshakes and file transfers are automatically recorded in System Logs."
            ),
            quickStartSteps = listOf(
                "1. Open Remote FTP Client from the dashboard.",
                "2. Tap 'Add Server Profile' and enter the Host IP/domain, Port (usually 21), and login credentials.",
                "3. Tap 'Connect'. Browse the remote filesystem.",
                "4. Tap any file to download it to your phone's Downloads directory."
            )
        ),
        ManualSection(
            id = "llm-gateway",
            title = "Local LLM Gateway & MCP Proxy",
            category = "AI & Local LLM Gateway",
            tagline = "Multi-account Cloud & Desktop LAN LLM proxy with smart failover and MCP.",
            iconEmoji = "🧠",
            accentColor = Emerald,
            toolRouteId = "llm-gateway",
            paragraphs = listOf(
                "The Local LLM Gateway is an embedded loopback proxy running on http://127.0.0.1:8080. It unifies cloud LLMs (Google Gemini, OpenAI ChatGPT) and local desktop LLM engines (Ollama, LM Studio, vLLM) behind a standard OpenAI-compatible API.",
                "It features automated Model Context Protocol (MCP) tool execution, seamless failover routing on rate limits or host drop, and automated LAN subnet scanning."
            ),
            keyFeatures = listOf(
                "OpenAI-Compatible REST API" to "POST /v1/chat/completions (JSON & SSE streaming), GET /v1/models, and GET /v1/mcp/* endpoints.",
                "Model Context Protocol (MCP)" to "Injects native device sensors, FTP status, system logs, and LAN MCP servers into LLM reasoning loops.",
                "Smart Failover Sequence" to "Automatically cascades failed requests from local desktop nodes (Ollama) to cloud accounts (Gemini/OpenAI) on HTTP 429 or connection loss.",
                "Subnet Scanner" to "One-tap LAN scanner that sweeps the /24 subnet to auto-discover active Ollama (:11434) and LM Studio (:1234) servers."
            ),
            quickStartSteps = listOf(
                "1. Open LLM Gateway from the dashboard.",
                "2. Start the Gateway Server (default :8080).",
                "3. Add your providers: tap 'Add Profile' to add Gemini API Key, OpenAI Key, or tap 'Discover LAN' to find desktop Ollama.",
                "4. Set your Priority #1 route in Settings > Default LLM.",
                "5. Any tool (CyberChat, Dynamic Tools Studio) or external app on the phone can now query http://127.0.0.1:8080/v1/chat/completions."
            ),
            codeSnippets = listOf(
                "Testing LLM Gateway via cURL" to """
                    curl -X POST http://127.0.0.1:8080/v1/chat/completions \
                      -H "Content-Type: application/json" \
                      -d '{
                        "model": "default",
                        "messages": [
                          {"role": "system", "content": "You are a concise assistant."},
                          {"role": "user", "content": "What is the capital of France?"}
                        ],
                        "temperature": 0.7
                      }'
                """.trimIndent()
            )
        ),
        ManualSection(
            id = "llm-chat",
            title = "CyberChat AI Studio",
            category = "AI & Local LLM Gateway",
            tagline = "Conversational AI client powered by LLM Gateway with MCP tool cards.",
            iconEmoji = "💬",
            accentColor = Cyan,
            toolRouteId = "llm-chat",
            paragraphs = listOf(
                "CyberChat AI is a cyberpunk-styled conversational interface powered by the local LLM Gateway. It features token streaming, multi-turn thread persistence, AI personas, and interactive MCP tool invocation cards."
            ),
            keyFeatures = listOf(
                "MCP Tool Cards" to "Renders interactive collapsible cards showing tool arguments and JSON returns when the model invokes sensors or system tools.",
                "AI Persona Presets" to "Code Architect (Kotlin/Android), Cyberpunk Operator (Security/Netrunner), Hardware Specialist (Sensors/NFC), and Creative Muse.",
                "Markdown & Syntax Highlighting" to "Monospace code blocks with 1-click 'Copy Code' actions.",
                "Persistent Thread History" to "Create, rename, search, and delete independent chat sessions stored safely in local JSON storage."
            ),
            tips = listOf(
                "Tip: Ask CyberChat 'What are my current device sensor readings?' to see live MCP tool calling in action!"
            )
        ),
        ManualSection(
            id = "sensors",
            title = "Sensors Live & Telemetry",
            category = "Hardware & Telemetry",
            tagline = "Real-time physical hardware sensor monitor with dynamic sampling.",
            iconEmoji = "📊",
            accentColor = Amber,
            toolRouteId = "sensors",
            paragraphs = listOf(
                "Sensors Live discovers and monitors every hardware sensor on your device (Accelerometer, Gyroscope, Magnetometer, Barometer, Light, Proximity, Temperature, Step Counter, etc.)."
            ),
            keyFeatures = listOf(
                "Dynamic Sampling Rates" to "Switch between 1s, 2s, 5s, Fast (Live), or Paused sampling rates on-the-fly.",
                "3-Axis Vector Decomposition" to "Real-time X, Y, Z coordinates with standard physical SI units (m/s², rad/s, µT, lx, hPa, °C).",
                "Telemetry Export" to "Copy full sensor snapshots formatted as Markdown tables or structured JSON directly to the clipboard."
            ),
            tips = listOf(
                "Tip: Tap 'Export Telemetry' to copy all real-time readings for diagnostics or research reports."
            )
        ),
        ManualSection(
            id = "log-viewer",
            title = "Centralized System Log Hub",
            category = "Diagnostics & System Logs",
            tagline = "Universal diagnostic bus and real-time operational stream.",
            iconEmoji = "📜",
            accentColor = Violet,
            toolRouteId = "log-viewer",
            paragraphs = listOf(
                "System Logs serves as the central nervous system of MOAA, indexing all file transfers, socket connections, NFC tag scans, LLM routing logs, and sensor alerts across the app."
            ),
            keyFeatures = listOf(
                "Universal Aggregator" to "Collects events from FTP Server, FTP Client, NFC Tools, LLM Gateway, Dynamic Tools Studio, and Sensors.",
                "Multi-Level Filtering" to "Filter logs by severity (VERBOSE, DEBUG, INFO, WARN, ERROR) and originating tool ID.",
                "Auto-Purge Retention Policy" to "Configurable memory retention (1 Hour, 1 Day, 1 Week, 1 Month, or Unlimited) from App Settings.",
                "Live Stream & Search" to "Real-time search bar with highlighted badges and 1-tap clipboard export."
            )
        ),
        ManualSection(
            id = "settings-shortcuts",
            title = "App Settings & Homescreen Shortcuts",
            category = "Shortcuts & System Settings",
            tagline = "Custom gallery columns, default LLM routing, MCP toggles, and launcher icons.",
            iconEmoji = "⚙️",
            accentColor = Cyan,
            toolRouteId = "settings",
            paragraphs = listOf(
                "The Settings tab lets you configure dashboard layouts, default LLM failover priorities, MCP tool servers, diagnostic memory policies, and pin one-tap launcher icons."
            ),
            keyFeatures = listOf(
                "Gallery Column Layout" to "Switch between 1 Column (detailed list), 2 Columns (isometric grid), 3 Columns (matrix), or 4 Columns (micro-deck).",
                "Default LLM Selector" to "Set your primary LLM routing target with one tap.",
                "MCP Server Toggles" to "Turn individual MCP tool servers on or off dynamically.",
                "Homescreen Shortcuts" to "Pin direct-launch shortcuts for any tool directly to your Android home screen launcher."
            )
        ),
        ManualSection(
            id = "terminal",
            title = "Cyber Terminal & Shell Emulator",
            category = "System & Developer Tools",
            tagline = "Linux / Ubuntu shell with command execution, pipes, and saved snippets.",
            iconEmoji = "💻",
            accentColor = Emerald,
            toolRouteId = "terminal",
            paragraphs = listOf(
                "Cyber Terminal provides an embedded Linux / Ubuntu shell on Android with interactive command execution, real-time streaming output, working directory tracking, and a persistent library of named commands and curl templates.",
                "Easily test local Ollama / LLM endpoints (e.g. 'curl http://127.0.0.1:11434/api/tags'), execute networking tools (ping, ip addr, netstat), inspect system specs (uname, df, ps), and save frequent commands to run in 1 click."
            ),
            keyFeatures = listOf(
                "Standard Linux Commands" to "Execute curl, ping, ip, ls, cat, grep, ps, uname, df, and custom shell pipelines.",
                "Stateful Directory Navigation" to "Built-in 'cd' tracking seamlessly updates your working directory across commands.",
                "Saved Commands & Templates" to "Save long commands with custom names and categories (AI & LLM, Networking, Storage, System) to run directly without retyping.",
                "Virtual Helper Keys" to "One-tap accessory keys (Tab, |, -, /, ~, $, >, curl, clear) and history up/down navigation for fast mobile typing.",
                "Real-Time Telemetry & Copy" to "Displays execution duration and exit codes with one-tap clipboard export."
            ),
            quickStartSteps = listOf(
                "1. Open Cyber Terminal from the Dashboard.",
                "2. Tap any preset chip at the top (e.g. '⚡ Ollama Tags' or '⚡ Ping DNS') to run immediately.",
                "3. Type any command into the prompt (e.g. 'curl -s http://127.0.0.1:8080/v1/models') and tap '▶ Run'.",
                "4. To save a command for later, tap '💾', give it a name and category, and tap 'Save Snippet'.",
                "5. Tap '⚡ Saved' in the top bar anytime to search and 1-click execute all your saved snippets."
            ),
            codeSnippets = listOf(
                "Local Ollama Models API Query" to "curl -s http://127.0.0.1:11434/api/tags | grep -o '\"name\":\"[^\"]*\"'",
                "Network & IP Address Inspection" to "ip addr show | grep -E 'inet |wlan0'",
                "Kernel Release & CPU Arch" to "uname -a && cat /proc/cpuinfo | grep 'model name' | head -n 4"
            ),
            tips = listOf(
                "Tip: You can use 'clear' or the trash icon in the top bar to reset the output screen anytime.",
                "Tip: Tap '⏹ Stop' in the top bar to cancel any long-running command (like ping or top)."
            )
        ),
        ManualSection(
            id = "system-info",
            title = "System Info & Hardware Telemetry",
            category = "Hardware & Telemetry",
            tagline = "Deep hardware, network, OS, memory, storage & kernel telemetry.",
            iconEmoji = "ℹ️",
            accentColor = Cyan,
            toolRouteId = "system-info",
            paragraphs = listOf(
                "System Info & Hardware provides comprehensive device and silicon inspection across 10 organized groups: OS & Android Build, Device & Hardware, CPU & Processor, RAM & Swap Memory, Storage Partitions, Network & Connectivity, Battery & Power, Display & Screen, Kernel & Linux Environment, and Security & DRM.",
                "All parameters can be individually copied to the clipboard with one tap, copied as an entire group, or exported as a full system diagnostics report. Features configurable live auto-refresh sampling intervals (1s, 3s, 5s, 10s, 1min, or Paused)."
            ),
            keyFeatures = listOf(
                "10 Grouped Metric Categories" to "Clean collapsible isometric cards with hardware progress bars (RAM %, Storage %, Battery %).",
                "1-Tap Individual & Group Copying" to "Copy any specific value (e.g. Wi-Fi IPv4, MAC address, Fingerprint) or copy an entire formatted group with 1 click.",
                "Full Report Export" to "Export the complete 10-category system report to your clipboard or diagnostic files via the top bar.",
                "Configurable Auto-Refresh Interval" to "Choose from 1s (Real-time), 3s, 5s (Balanced), 10s, 1min, or Manual/Paused ticker sampling.",
                "Instant Search & Filter" to "Filter all metrics across all categories in real time."
            ),
            quickStartSteps = listOf(
                "1. Open System Info from the Dashboard.",
                "2. Tap any metric row to copy its exact value to your clipboard.",
                "3. Tap '📋 Copy' on any group card header to copy the entire group's formatted key-value pairs.",
                "4. Tap the '⏱️ Auto-Refresh' dropdown in the top controls to adjust the live telemetry update speed.",
                "5. Tap '📋' in the top bar to copy the full multi-group system diagnostics report."
            ),
            codeSnippets = listOf(
                "Querying Memory Info from Android Shell" to "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable|SwapTotal'",
                "Extracting Wi-Fi Interface & Hardware MAC" to "ip addr show wlan0 || cat /sys/class/net/wlan0/address"
            ),
            tips = listOf(
                "Tip: Use the search bar at the top to instantly find specific values like 'mac', 'ip', 'fingerprint', or 'ram'.",
                "Tip: Switch to '1s (Real-time)' mode to monitor live memory allocations during intensive operations."
            )
        ),
        ManualSection(
            id = "net-topology",
            title = "Network Topology & Scanner",
            category = "Networking & Security",
            tagline = "Interactive visual network map, ARP discovery, multi-hop topology & port scanner.",
            iconEmoji = "🌐",
            accentColor = Color(0xFF06B6D4),
            toolRouteId = "net-topology",
            paragraphs = listOf(
                "Network Topology & Scanner performs high-speed multi-protocol network discovery across your active LAN subnet. It concurrently combines ICMP ping, TCP socket probes, and kernel ARP tables (/proc/net/arp) to identify all reachable devices including routers, secondary mesh APs, PCs, smartphones, servers/NAS, smart home IoT microcontrollers (ESP32, Tuya), and network printers.",
                "Discovered nodes are rendered in an interactive 2D Canvas supporting Hierarchical Tree, Radial Orbital, and Concentric Mesh layouts with animated signal particles and latency halos. Tapping any node opens a deep inspection sheet with MAC OUI manufacturer identification, open TCP port scanning, live hop traceroute probing, and full network report export."
            ),
            keyFeatures = listOf(
                "Interactive Visual Topology Graph" to "Zoomable and pannable 2D Canvas with 3 layout modes (Tree Hierarchy, Orbital Radar, Concentric Mesh) and live signal flow animations.",
                "Multi-Protocol Subnet Scanner" to "High-speed parallel ICMP ping, TCP socket probing, and ARP cache parsing to detect devices even if ICMP ping is firewalled.",
                "MAC Vendor OUI Database" to "Embedded OUI resolver identifying device manufacturers (Apple, Samsung, Google, Espressif, Raspberry Pi, TP-Link, Cisco, Netgear, Xiaomi, etc.).",
                "Open Port & Service Scanner" to "Probes common network ports (HTTP, HTTPS, SSH, FTP, RTSP, MQTT, MySQL, Ollama LLM, Printers) with banner detection.",
                "Live Traceroute & Hop Probing" to "Inspects intermediate routing hops and serial routers/hubs between your device and the target node.",
                "Dual Graph & List View" to "Seamlessly switch between an interactive visual graph map and a detailed searchable list with 1-tap copy actions."
            ),
            quickStartSteps = listOf(
                "1. Open Network Topology & Scanner from the Dashboard while connected to Wi-Fi or Ethernet.",
                "2. Tap the Refresh icon in the top bar to run a full subnet discovery scan.",
                "3. In Graph View, pinch to zoom, drag to pan, and tap any node to inspect its technical properties.",
                "4. Use the layout switcher (🌳 / 🎯 / 🕸️) to toggle between Tree, Radial, and Mesh graph structures.",
                "5. In the Deep Inspect sheet, tap '⚡ Deep Scan' to discover open ports or '🛰️ Trace Route' to map hops.",
                "6. Tap the Export icon in the top bar to copy the full multi-node network report to your clipboard."
            ),
            codeSnippets = listOf(
                "Parsing ARP Cache from Linux Shell" to "cat /proc/net/arp",
                "Subnet Scan using Ping & Nmap" to "nmap -sn 192.168.1.0/24 || ping -c 1 192.168.1.1"
            ),
            tips = listOf(
                "Tip: If a device has HTTP (port 80/8080) or HTTPS (port 443) open, tap '🌐 Open Web' in the inspector to directly launch its web admin portal.",
                "Tip: In List View, devices start collapsed by default for a clean view — tap any row to expand its MAC, open ports, and quick actions."
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemManualScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedSectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val categories = listOf("All") + SystemManualData.SECTIONS.map { it.category }.distinct()

    val filteredSections = SystemManualData.SECTIONS.filter { section ->
        val matchesCategory = selectedCategory == "All" || section.category == selectedCategory
        val matchesSearch = if (searchQuery.isBlank()) true else {
            section.title.contains(searchQuery, ignoreCase = true) ||
                    section.tagline.contains(searchQuery, ignoreCase = true) ||
                    section.paragraphs.any { it.contains(searchQuery, ignoreCase = true) } ||
                    section.keyFeatures.any { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
        }
        matchesCategory && matchesSearch
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SYSTEM USER MANUAL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Comprehensive Architecture & Tool Guide",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tools, protocols, APIs, features...", fontSize = 12.sp, color = TextSecondary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Cyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassSurfaceDeep,
                        unfocusedContainerColor = GlassSurfaceDeep,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // Category Chips Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Cyan.copy(alpha = 0.2f) else Color(0x201E293B),
                            border = BorderStroke(1.dp, if (isSelected) Cyan else GlassBorder)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Cyan else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Search Results Counter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL TOPICS (${filteredSections.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )

                    Text(
                        text = if (expandedSectionIds.size == filteredSections.size) "Collapse All" else "Expand All",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                expandedSectionIds = if (expandedSectionIds.size == filteredSections.size) {
                                    emptySet()
                                } else {
                                    filteredSections.map { it.id }.toSet()
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Sections
            if (filteredSections.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 28.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No manual topics match '$searchQuery'",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try searching for another keyword (e.g., 'FTP', 'NFC', 'Bridge', 'MCP', 'LLM').",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredSections, key = { it.id }) { section ->
                    val isExpanded = expandedSectionIds.contains(section.id)

                    ManualSectionCard(
                        section = section,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedSectionIds = if (isExpanded) {
                                expandedSectionIds - section.id
                            } else {
                                expandedSectionIds + section.id
                            }
                        },
                        onLaunchTool = {
                            section.toolRouteId?.let { onNavigateToTool(it) }
                        },
                        onCopySnippet = { label, code ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(label, code))
                            Toast.makeText(context, "Copied $label to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualSectionCard(
    section: ManualSection,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLaunchTool: () -> Unit,
    onCopySnippet: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (isExpanded) section.accentColor.copy(alpha = 0.5f) else GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = section.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, section.accentColor.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(section.iconEmoji, fontSize = 18.sp)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = section.accentColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = section.category.uppercase(),
                                    color = section.accentColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = section.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = section.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Paragraphs
                    section.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Key Features
                    if (section.keyFeatures.isNotEmpty()) {
                        Text(
                            text = "KEY CAPABILITIES & ARCHITECTURE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = section.accentColor,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            section.keyFeatures.forEach { (feature, desc) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x201E293B),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "• $feature",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = section.accentColor,
                                            fontSize = 10.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Start Steps
                    if (section.quickStartSteps.isNotEmpty()) {
                        Text(
                            text = "QUICK START WORKFLOW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = section.accentColor,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x201E293B),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                section.quickStartSteps.forEach { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Code Snippets
                    if (section.codeSnippets.isNotEmpty()) {
                        section.codeSnippets.forEach { (label, code) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = section.accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "📋 Copy Code",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Cyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onCopySnippet(label, code) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF030712),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            lineHeight = 13.sp,
                                            color = Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tips
                    if (section.tips.isNotEmpty()) {
                        section.tips.forEach { tip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = section.accentColor.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, section.accentColor.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = section.accentColor,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    // Quick Launch Action (if toolRouteId exists)
                    if (section.toolRouteId != null) {
                        Button(
                            onClick = onLaunchTool,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = section.accentColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Open",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "OPEN ${section.title.uppercase()} IN APP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun SystemManualScreenPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        SystemManualScreen(
            onNavigateBack = {},
            onNavigateToTool = {}
        )
    }
}

