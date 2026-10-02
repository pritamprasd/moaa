package dev.motherofallapps.host.tool.llmgateway.connector

import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionResponse
import dev.motherofallapps.host.tool.llmgateway.model.ChatStreamChunk
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
import kotlinx.coroutines.flow.Flow

data class PingResult(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val models: List<String> = emptyList(),
    val errorMessage: String? = null
)

interface LlmConnector {
    suspend fun executeChat(profile: LlmProfile, request: ChatCompletionRequest): ChatCompletionResponse
    fun streamChat(profile: LlmProfile, request: ChatCompletionRequest): Flow<ChatStreamChunk>
    suspend fun ping(profile: LlmProfile): PingResult
}
