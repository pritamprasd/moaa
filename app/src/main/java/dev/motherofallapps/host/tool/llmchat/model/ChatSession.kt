package dev.motherofallapps.host.tool.llmchat.model

import java.util.UUID

/**
 * Multi-turn chat conversation session.
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val personaId: String = PersonaPreset.DEFAULT_ASSISTANT.id,
    val systemPrompt: String = PersonaPreset.DEFAULT_ASSISTANT.systemPrompt,
    val temperature: Float = 0.7f,
    val targetModelOverride: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
)
