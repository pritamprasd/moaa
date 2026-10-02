package dev.motherofallapps.host.tool.llmgateway.model

import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolResult

enum class ProviderCategory(val displayName: String, val icon: String) {
    CLOUD_OAUTH("Cloud Account (OAuth / Key)", "☁️"),
    DESKTOP_LOCAL_HOST("Desktop Local LAN Host", "🖥️")
}

enum class ProfileStatus(val displayName: String, val badgeColorHex: Long) {
    ACTIVE("Active / Online", 0xFF34D399),       // Emerald
    RATE_LIMITED("Rate Limited (429)", 0xFFF59E0B), // Amber
    HOST_UNREACHABLE("Host Unreachable", 0xFFF43F5E), // Rose
    EXPIRED("Auth Token Expired", 0xFFA855F7), // Purple
    IDLE("Idle / Standby", 0xFF94A3B8)         // Slate
}

data class LlmProfile(
    val id: String,
    val name: String, // e.g. "Home Desktop Ollama" or "Personal Gemini Pro"
    val category: ProviderCategory,
    val providerType: String, // GEMINI_CLOUD, CHATGPT_CLOUD, OLLAMA_LOCAL, LM_STUDIO, CUSTOM_OPENAI

    // Cloud Auth Fields
    val accountEmail: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val apiKey: String? = null,

    // Desktop Host Fields
    val hostAddress: String? = null, // e.g. "http://192.168.1.50:11434"
    val targetModel: String? = null, // e.g. "llama3.2:latest", "gemini-1.5-flash", "gpt-4o-mini"

    val status: ProfileStatus = ProfileStatus.IDLE,
    val latencyMs: Long = 0L,
    val priorityOrder: Int = 0,
    val isEnabled: Boolean = true,
    val quotaRemaining: Int? = null,
    val quotaLimit: Int? = null
)

sealed class RouterResult<out T> {
    data class Success<T>(val data: T, val profileUsed: String) : RouterResult<T>()
    data class FallbackSuccess<T>(
        val data: T,
        val attemptedProfiles: List<String>,
        val finalProfile: String
    ) : RouterResult<T>()
    data class AllTargetsExhausted(val errors: Map<String, String>) : RouterResult<Nothing>()
}

data class ChatMessage(
    val role: String, // "system", "user", "assistant", "tool"
    val content: String,
    val toolCalls: List<McpToolCall> = emptyList(),
    val toolCallId: String? = null
)

data class ChatCompletionRequest(
    val model: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val temperature: Double? = 0.7,
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    val tools: List<McpToolDefinition> = emptyList(),
    val autoExecuteTools: Boolean = true
)

data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val content: String,
    val profileUsed: String,
    val latencyMs: Long,
    val fallbackAttempted: List<String> = emptyList(),
    val toolCalls: List<McpToolCall> = emptyList(),
    val toolResults: List<McpToolResult> = emptyList()
)

data class ChatStreamChunk(
    val id: String,
    val model: String,
    val deltaContent: String,
    val finishReason: String? = null,
    val profileUsed: String,
    val fallbackAttempted: List<String> = emptyList(),
    val toolCalls: List<McpToolCall> = emptyList(),
    val toolResults: List<McpToolResult> = emptyList()
)

data class DiscoveredHost(
    val hostIp: String,
    val port: Int,
    val serviceType: String, // "Ollama", "LM Studio", "vLLM / LocalAI"
    val fullUrl: String,
    val modelsAvailable: List<String> = emptyList(),
    val responseTimeMs: Long = 0L
)
