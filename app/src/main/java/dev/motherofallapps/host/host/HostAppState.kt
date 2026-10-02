package dev.motherofallapps.host.host

import dev.motherofallapps.pluginapi.ToolId
import dev.motherofallapps.pluginapi.ToolInfo
import dev.motherofallapps.pluginapi.ToolState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HostAppState {
    private val _tools = MutableStateFlow<List<ToolInfo>>(
        listOf(
            ToolInfo(
                id = ToolId("ftp-server"),
                name = "LAN FTP Server",
                description = "Host phone storage over local Wi-Fi with custom credentials, background execution, connected device tracking, and live telemetry.",
                version = "1.0.0",
                state = ToolState.INSTALLED,
            ),
            ToolInfo(
                id = ToolId("nfc-tool"),
                name = "NFC Tag Master",
                description = "Read & write NFC tags (NTAG, Mifare, DESFire), create NDEF records, and explore features of popular NFC tools.",
                version = "1.0.0",
                state = ToolState.INSTALLED,
            ),
            ToolInfo(
                id = ToolId("log-viewer"),
                name = "System & Tool Log Viewer",
                description = "Centralized real-time debug console and diagnostic logs for all active and installed tools.",
                version = "1.0.0",
                state = ToolState.INSTALLED,
            )
        )
    )
    val tools: StateFlow<List<ToolInfo>> = _tools.asStateFlow()
}