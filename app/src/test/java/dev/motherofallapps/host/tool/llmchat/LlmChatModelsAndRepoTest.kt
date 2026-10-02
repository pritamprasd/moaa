package dev.motherofallapps.host.tool.llmchat

import dev.motherofallapps.host.tool.llmchat.model.ChatMessage
import dev.motherofallapps.host.tool.llmchat.model.ChatSession
import dev.motherofallapps.host.tool.llmchat.model.PersonaPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LlmChatModelsAndRepoTest {

    @Test
    fun testChatMessageCreationAndDefaults() {
        val message = ChatMessage(
            role = "user",
            content = "Explain Kotlin coroutines",
            modelUsed = "gemini-1.5-flash",
            providerUsed = "Gemini Cloud",
            latencyMs = 450,
            failoverTrail = listOf("Ollama Local", "Gemini Cloud")
        )

        assertNotNull(message.id)
        assertEquals("user", message.role)
        assertEquals("Explain Kotlin coroutines", message.content)
        assertEquals("gemini-1.5-flash", message.modelUsed)
        assertEquals("Gemini Cloud", message.providerUsed)
        assertEquals(450L, message.latencyMs)
        assertEquals(2, message.failoverTrail.size)
        assertFalse(message.isStreaming)
        assertFalse(message.isError)
    }

    @Test
    fun testChatSessionCreationAndDefaults() {
        val session = ChatSession(
            id = "sess-123",
            title = "Architecture Discussion",
            personaId = PersonaPreset.CODE_ARCHITECT.id,
            systemPrompt = PersonaPreset.CODE_ARCHITECT.systemPrompt,
            temperature = 0.2f,
            messages = listOf(
                ChatMessage(role = "user", content = "Design an Android Clean Architecture layer.")
            )
        )

        assertEquals("sess-123", session.id)
        assertEquals("Architecture Discussion", session.title)
        assertEquals("code_architect", session.personaId)
        assertEquals(0.2f, session.temperature, 0.001f)
        assertEquals(1, session.messages.size)
    }

    @Test
    fun testPersonaPresetsCatalog() {
        val allPresets = PersonaPreset.ALL_PRESETS
        assertTrue(allPresets.isNotEmpty())
        assertEquals(5, allPresets.size)

        val coder = PersonaPreset.findById("code_architect")
        assertEquals(PersonaPreset.CODE_ARCHITECT, coder)
        assertEquals(0.2f, coder.defaultTemperature, 0.001f)

        val hacker = PersonaPreset.findById("cyber_operator")
        assertEquals(PersonaPreset.CYBER_OPERATOR, hacker)

        val fallback = PersonaPreset.findById("non-existent-id")
        assertEquals(PersonaPreset.DEFAULT_ASSISTANT, fallback)
    }

    @Test
    fun testSessionMessageCopyAndUpdate() {
        val msgId = UUID.randomUUID().toString()
        val originalMsg = ChatMessage(
            id = msgId,
            role = "assistant",
            content = "Hel",
            isStreaming = true
        )

        val updatedMsg = originalMsg.copy(
            content = "Hello, how can I assist you?",
            isStreaming = false,
            latencyMs = 320
        )

        assertEquals(msgId, updatedMsg.id)
        assertEquals("Hello, how can I assist you?", updatedMsg.content)
        assertFalse(updatedMsg.isStreaming)
        assertEquals(320L, updatedMsg.latencyMs)
    }
}
