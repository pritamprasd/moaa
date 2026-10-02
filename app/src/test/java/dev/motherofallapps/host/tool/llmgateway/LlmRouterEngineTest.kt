package dev.motherofallapps.host.tool.llmgateway

import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.ChatMessage
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
import dev.motherofallapps.host.tool.llmgateway.model.ProfileStatus
import dev.motherofallapps.host.tool.llmgateway.model.ProviderCategory
import dev.motherofallapps.host.tool.llmgateway.model.RouterResult
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
}
