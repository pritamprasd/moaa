package dev.motherofallapps.host.tool.llmchat.model

import java.util.UUID

/**
 * Single chat message within a session.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val providerUsed: String? = null,
    val latencyMs: Long = 0,
    val failoverTrail: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
)
