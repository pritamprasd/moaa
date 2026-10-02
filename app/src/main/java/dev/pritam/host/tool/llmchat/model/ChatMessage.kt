package dev.pritam.host.tool.llmchat.model

import dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import java.util.UUID

/**
 * Single chat message within a session with support for MCP tool calls.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val providerUsed: String? = null,
    val latencyMs: Long = 0,
    val failoverTrail: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val toolCalls: List<McpToolCall> = emptyList(),
    val toolResults: List<McpToolResult> = emptyList()
)
