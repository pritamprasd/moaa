package dev.motherofallapps.host.config

import dev.motherofallapps.pluginapi.ToolId
import dev.motherofallapps.pluginapi.ToolInfo
import dev.motherofallapps.pluginapi.ToolState

/**
 * Static metadata configuration for all tools hosted in MotherOfAllApps.
 * Developers and maintainers can easily customize tool names, taglines,
 * descriptions, versions, categories, and permissions in this single registry.
 */
data class ToolDefinition(
    val id: String,
    val name: String,
    val shortTagline: String,
    val description: String,
    val version: String,
    val category: String,
    val author: String = "MotherOfAllApps Core",
    val iconType: String,
    val accentColorHex: Long,
    val requiredPermissions: List<String> = emptyList(),
    val isEnabled: Boolean = true,
) {
    fun toToolInfo(): ToolInfo {
        return ToolInfo(
            id = ToolId(id),
            name = name,
            description = shortTagline,
            version = version,
            state = if (isEnabled) ToolState.INSTALLED else ToolState.AVAILABLE
        )
    }
}

object ToolRegistryConfig {

    /**
     * The master list of tools. Update any tool name, description, or version here.
     */
    val INSTALLED_TOOLS: List<ToolDefinition> = listOf(
        ToolDefinition(
            id = "ftp-server",
            name = "LAN FTP Server",
            shortTagline = "Host local phone storage over Wi-Fi with custom credentials.",
            description = "High-performance embedded FTP server engine. Allows devices on your Local Area Network (Windows Explorer, macOS Finder, mobile FTP apps) to browse, download, and upload files from phone storage with background execution support.",
            version = "1.0.0",
            category = "Networking & File Transfer",
            author = "Core Systems",
            iconType = "ftp",
            accentColorHex = 0xFF38BDF8, // Cyan
            requiredPermissions = listOf("INTERNET", "ACCESS_WIFI_STATE", "MANAGE_EXTERNAL_STORAGE", "FOREGROUND_SERVICE")
        ),
        ToolDefinition(
            id = "nfc-tool",
            name = "NFC Tag Master",
            shortTagline = "Read, write & inspect NFC tags with NDEF encoding.",
            description = "Complete contactless RFID/NFC toolkit. Read and diagnose silicon ICs (NTAG213/215/216, Mifare Classic, DESFire), write custom NDEF payloads (Text, URL, Wi-Fi credentials, vCard contacts, App Launchers), and explore popular NFC app capabilities.",
            version = "1.0.0",
            category = "Hardware & Contactless",
            author = "Hardware Labs",
            iconType = "nfc",
            accentColorHex = 0xFFF472B6, // Rose
            requiredPermissions = listOf("NFC")
        ),
        ToolDefinition(
            id = "ftp-client",
            name = "FTP Client",
            shortTagline = "Mount remote FTP servers, manage files & remember credentials.",
            description = "Full-featured remote FTP client. Connect and mount remote FTP servers over LAN/WAN, remember server profiles and credentials, browse remote directories, upload/download files with progress telemetry, create/delete directories, rename and transfer files with full System Logs integration.",
            version = "1.0.0",
            category = "Networking & File Transfer",
            author = "Network Core",
            iconType = "ftp-client",
            accentColorHex = 0xFF34D399, // Emerald
            requiredPermissions = listOf("INTERNET", "ACCESS_NETWORK_STATE")
        ),
        ToolDefinition(
            id = "log-viewer",
            name = "System Logs",
            shortTagline = "Centralized real-time diagnostic and operation stream.",
            description = "Universal diagnostic hub and telemetry console. Collects, filters, searches, and exports real-time execution logs, file transfers, operations, and NFC events across the super-app.",
            version = "1.0.0",
            category = "Diagnostics & System Logs",
            author = "System Core",
            iconType = "logs",
            accentColorHex = 0xFFA78BFA, // Violet
            requiredPermissions = listOf("None")
        ),
        ToolDefinition(
            id = "sensors",
            name = "Sensors Live",
            shortTagline = "Real-time hardware sensor monitor with dynamic 1s/2s/5s sampling.",
            description = "Discover and inspect all physical hardware sensors available on this device (Accelerometer, Gyroscope, Magnetometer, Barometer, Light, Proximity, Temperature, Step Counter, etc.). Features configurable live update intervals (1s, 2s, 5s, fast sampling, or paused), real-time telemetry meters, multi-axis decomposition, hardware specs, and telemetry export.",
            version = "1.0.0",
            category = "Hardware & Telemetry",
            author = "Hardware Core",
            iconType = "sensors",
            accentColorHex = 0xFFF59E0B, // Amber
            requiredPermissions = listOf("None")
        ),
        ToolDefinition(
            id = "llm-gateway",
            name = "LLM Gateway",
            shortTagline = "Multi-account Cloud & Desktop LAN LLM proxy with smart failover.",
            description = "High-performance authenticated loopback HTTP proxy (http://127.0.0.1:8080) for Gemini Pro/Flash, OpenAI ChatGPT, and local LAN desktop engines (Ollama, LM Studio, vLLM). Features automatic failover on rate limits (429) or host drop, LAN mDNS/subnet discovery, and priority sequence management.",
            version = "1.0.0",
            category = "AI & Local LLM Gateway",
            author = "AI Core",
            iconType = "brain",
            accentColorHex = 0xFF10B981, // Emerald
            requiredPermissions = listOf("INTERNET", "ACCESS_NETWORK_STATE", "ACCESS_WIFI_STATE")
        ),
        ToolDefinition(
            id = "llm-chat",
            name = "CyberChat AI",
            shortTagline = "Local LLM chat studio powered by LLM Gateway failover proxy.",
            description = "Full-featured interactive conversational AI client powered by the LLM Gateway loopback proxy. Features real-time token streaming, multi-turn thread history management, customizable AI personas (Code Architect, Cyberpunk Hacker, Hardware Specialist, Creative Muse), markdown & code formatting with one-click copy, temperature sliders, and instant failover between Cloud Gemini/OpenAI and Desktop Ollama.",
            version = "1.0.0",
            category = "AI & Local LLM Gateway",
            author = "AI Core",
            iconType = "chat",
            accentColorHex = 0xFF38BDF8, // Sky Cyan
            requiredPermissions = listOf("INTERNET", "ACCESS_NETWORK_STATE")
        )
    )

    fun getRegisteredToolInfos(): List<ToolInfo> {
        return INSTALLED_TOOLS.filter { it.isEnabled }.map { it.toToolInfo() }
    }

    fun findToolDefinition(toolId: String): ToolDefinition? {
        return INSTALLED_TOOLS.firstOrNull { it.id == toolId }
    }
}
