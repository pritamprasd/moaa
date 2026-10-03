package dev.pritam.host.config

import android.content.Context
import dev.pritam.pluginapi.ToolId
import dev.pritam.pluginapi.ToolInfo
import dev.pritam.pluginapi.ToolState

/**
 * Metadata configuration for all tools hosted in MotherOfAllApps.
 *
 * Single Source of Truth: `config.yaml` (located at repo root and synced to assets).
 * Tool names, taglines, descriptions, icons, emojis, versions, categories,
 * and permissions are defined in `config.yaml` and parsed at runtime.
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
    val emoji: String = "🔧",
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

    @Volatile
    private var cachedTools: List<ToolDefinition>? = null

    /**
     * Primary tools list loaded from single-source-of-truth `config.yaml`.
     */
    val INSTALLED_TOOLS: List<ToolDefinition>
        get() {
            return cachedTools ?: synchronized(this) {
                cachedTools ?: loadToolsFromConfig().also { cachedTools = it }
            }
        }

    /**
     * Explicit initializer for Android runtime to load `config.yaml` from assets.
     */
    fun init(context: Context) {
        try {
            context.assets.open("config.yaml").use { stream ->
                val parsed = YamlToolConfigParser.parse(stream)
                if (parsed.isNotEmpty()) {
                    synchronized(this) {
                        cachedTools = parsed
                    }
                }
            }
        } catch (_: Exception) {
            // Asset reading fallback handled by loadToolsFromConfig
        }
    }

    private fun loadToolsFromConfig(): List<ToolDefinition> {
        // 1. Try ClassLoader resource (present in APK resources and JVM test classpath)
        try {
            ToolRegistryConfig::class.java.classLoader?.getResourceAsStream("config.yaml")?.use { stream ->
                val parsed = YamlToolConfigParser.parse(stream)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (_: Exception) {}

        // 2. Try file system relative paths (for Gradle/JVM test runner environments)
        val candidatePaths = listOf(
            "config.yaml",
            "../config.yaml",
            "app/src/main/assets/config.yaml",
            "app/src/main/resources/config.yaml"
        )
        for (path in candidatePaths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.isFile) {
                    val parsed = YamlToolConfigParser.parse(file.readText())
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback bundled tools in the unlikely event file access is restricted
        return FALLBACK_TOOLS
    }

    fun getRegisteredToolInfos(): List<ToolInfo> {
        return INSTALLED_TOOLS.filter { it.isEnabled }.map { it.toToolInfo() }
    }

    fun findToolDefinition(toolId: String): ToolDefinition? {
        val staticDef = INSTALLED_TOOLS.firstOrNull { it.id == toolId }
        if (staticDef != null) return staticDef

        if (toolId.startsWith("dynamic_")) {
            val rawName = toolId.removePrefix("dynamic_").replace("_", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            return ToolDefinition(
                id = toolId,
                name = rawName,
                shortTagline = "Dynamic AI Web Tool running in sandboxed container.",
                description = "Self-contained HTML/CSS/JS dynamic tool running in sandboxed WebView with full native AndroidBridge integration.",
                version = "1.0.0",
                category = "Dynamic Web Tools",
                author = "Dynamic AI Generator",
                iconType = "dynamic-tool",
                emoji = "⚡",
                accentColorHex = 0xFF38BDF8
            )
        }
        return null
    }

    private val FALLBACK_TOOLS = listOf(
        ToolDefinition("ftp-server", "FTP Server", "Host local phone storage over Wi-Fi with custom credentials.", "High-performance embedded FTP server engine.", "1.0.0", "Networking & File Transfer", "Core Systems", "ftp", "📡", 0xFF38BDF8),
        ToolDefinition("nfc-tool", "NFC Tools", "Read, write & inspect NFC tags with NDEF encoding.", "Complete contactless RFID/NFC toolkit.", "1.0.0", "Hardware & Contactless", "Hardware Labs", "nfc", "📶", 0xFFF472B6),
        ToolDefinition("ftp-client", "FTP Client", "Mount remote FTP servers, manage files & remember credentials.", "Full-featured remote FTP client.", "1.0.0", "Networking & File Transfer", "Network Core", "ftp-client", "☁️", 0xFF34D399),
        ToolDefinition("log-viewer", "Logs", "Centralized real-time diagnostic and operation stream.", "Universal diagnostic hub and telemetry console.", "1.0.0", "Diagnostics & System Logs", "System Core", "logs", "📋", 0xFFA78BFA),
        ToolDefinition("sensors", "Sensors", "Real-time hardware sensor monitor with dynamic 1s/2s/5s sampling.", "Discover and inspect all physical hardware sensors.", "1.0.0", "Hardware & Telemetry", "Hardware Core", "sensors", "🧭", 0xFFF59E0B),
        ToolDefinition("llm-gateway", "LLM Gateway", "Multi-account Cloud & Desktop LAN LLM proxy with smart failover.", "Loopback HTTP proxy for LLMs.", "1.0.0", "AI & Local LLM Gateway", "AI Core", "brain", "🧠", 0xFF10B981),
        ToolDefinition("llm-chat", "Chat", "Local LLM chat studio powered by LLM Gateway failover proxy.", "Interactive conversational AI client.", "1.0.0", "AI & Local LLM Gateway", "AI Core", "chat", "💬", 0xFF38BDF8),
        ToolDefinition("dynamic-tools-studio", "Custom Tools", "Synthesize and run custom web tools with AI prompts.", "AI-powered dynamic custom tool generator.", "1.0.0", "AI & Dynamic Web Tools", "AI Tools Core", "dynamic-tool", "⚡", 0xFFF43F5E),
        ToolDefinition("system-manual", "System Manual", "Comprehensive guide & user manual for all tools and architecture.", "In-app documentation and user guide.", "1.0.0", "Documentation & Guides", "Documentation Core", "manual", "📖", 0xFF38BDF8),
        ToolDefinition("ghost-agent", "Ghost Agent", "Background AI agent that automates multi-step tasks across any app.", "Intelligent background automation agent.", "1.0.0", "AI & Automation", "Ghost Labs", "ghost", "👻", 0xFFA78BFA),
        ToolDefinition("terminal", "Cyber Terminal", "Linux shell emulator with command execution, pipes & saved snippets.", "Full-featured Linux terminal emulator.", "1.0.0", "System & Developer Tools", "Core Systems", "terminal", "💻", 0xFF10B981),
        ToolDefinition("system-info", "System Info & Hardware", "Deep hardware, network, OS, memory, storage & kernel telemetry.", "Comprehensive device and hardware inspection toolkit.", "1.0.0", "Hardware & Telemetry", "Hardware Labs", "system-info", "ℹ️", 0xFF38BDF8),
        ToolDefinition("net-topology", "Network Topology & Scanner", "Interactive visual network map, ARP discovery, multi-hop topology & port scanner.", "Local network scanner and visual topology mapper.", "1.0.0", "Networking & Security", "Network Core", "net-topology", "🌐", 0xFF06B6D4)
    )
}
