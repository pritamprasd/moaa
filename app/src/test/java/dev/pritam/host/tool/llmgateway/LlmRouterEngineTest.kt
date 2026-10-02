package dev.pritam.host.tool.llmgateway

import dev.pritam.host.tool.llmgateway.model.ChatCompletionRequest
import dev.pritam.host.tool.llmgateway.model.ChatMessage
import dev.pritam.host.tool.llmgateway.model.LlmProfile
import dev.pritam.host.tool.llmgateway.model.ProfileStatus
import dev.pritam.host.tool.llmgateway.model.ProviderCategory
import dev.pritam.host.tool.llmgateway.model.RouterResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmRouterEngineTest {

    @Test
    fun testLlmProfileCreationAndDefaults() {
        val profile = LlmProfile(
            id = "test-ollama",
            name = "Test Ollama",
            category = ProviderCategory.DESKTOP_LOCAL_HOST,
            providerType = "OLLAMA_LOCAL",
            hostAddress = "http://192.168.1.50:11434",
            targetModel = "llama3.2:latest",
            status = ProfileStatus.IDLE
        )

        assertEquals("test-ollama", profile.id)
        assertEquals(ProviderCategory.DESKTOP_LOCAL_HOST, profile.category)
        assertEquals("http://192.168.1.50:11434", profile.hostAddress)
        assertTrue(profile.isEnabled)
    }

    @Test
    fun testRouterResultContract() {
        val success: RouterResult<String> = RouterResult.Success(
            data = "Hello from Ollama",
            profileUsed = "Ollama-Local"
        )
        assertTrue(success is RouterResult.Success)
        assertEquals("Hello from Ollama", (success as RouterResult.Success).data)
        assertEquals("Ollama-Local", success.profileUsed)

        val fallback: RouterResult<String> = RouterResult.FallbackSuccess(
            data = "Hello from Gemini",
            attemptedProfiles = listOf("Ollama-Local"),
            finalProfile = "Gemini-Pro"
        )
        assertTrue(fallback is RouterResult.FallbackSuccess)
        val fb = fallback as RouterResult.FallbackSuccess
        assertEquals(listOf("Ollama-Local"), fb.attemptedProfiles)
        assertEquals("Gemini-Pro", fb.finalProfile)

        val exhausted: RouterResult<String> = RouterResult.AllTargetsExhausted(
            errors = mapOf("Ollama" to "ConnectException", "Gemini" to "429 Quota Exceeded")
        )
        assertTrue(exhausted is RouterResult.AllTargetsExhausted)
        assertEquals(2, (exhausted as RouterResult.AllTargetsExhausted).errors.size)
    }

    @Test
    fun testChatCompletionRequestParsing() {
        val request = ChatCompletionRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                ChatMessage("system", "You are a helpful assistant"),
                ChatMessage("user", "Hello world")
            ),
            temperature = 0.7,
            maxTokens = 100,
            stream = false
        )

        assertEquals("gpt-4o-mini", request.model)
        assertEquals(2, request.messages.size)
        assertEquals("system", request.messages[0].role)
        assertEquals("user", request.messages[1].role)
    }

    @Test
    fun testDefaultLlmPriorityOrdering() {
        val profiles = listOf(
            LlmProfile(
                id = "p1",
                name = "Gemini",
                category = ProviderCategory.CLOUD_OAUTH,
                providerType = "GEMINI_CLOUD",
                priorityOrder = 0,
                isEnabled = true
            ),
            LlmProfile(
                id = "p2",
                name = "Ollama",
                category = ProviderCategory.DESKTOP_LOCAL_HOST,
                providerType = "OLLAMA_LOCAL",
                priorityOrder = 1,
                isEnabled = true
            ),
            LlmProfile(
                id = "p3",
                name = "ChatGPT",
                category = ProviderCategory.CLOUD_OAUTH,
                providerType = "CHATGPT_CLOUD",
                priorityOrder = 2,
                isEnabled = true
            )
        )

        // Select p3 (ChatGPT) as default -> moves to top (priority 0)
        val current = profiles.toMutableList()
        val index = current.indexOfFirst { it.id == "p3" }
        val target = current.removeAt(index).copy(isEnabled = true)
        current.add(0, target)
        current.forEachIndexed { idx, p -> current[idx] = p.copy(priorityOrder = idx) }

        assertEquals("p3", current[0].id)
        assertEquals(0, current[0].priorityOrder)
        assertEquals("p1", current[1].id)
        assertEquals(1, current[1].priorityOrder)
        assertEquals("p2", current[2].id)
        assertEquals(2, current[2].priorityOrder)
    }
}

