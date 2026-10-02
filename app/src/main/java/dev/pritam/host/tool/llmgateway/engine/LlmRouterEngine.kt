package dev.pritam.host.tool.llmgateway.engine

import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.llmgateway.connector.ChatGptCloudConnector
import dev.pritam.host.tool.llmgateway.connector.DesktopLocalConnector
import dev.pritam.host.tool.llmgateway.connector.GeminiCloudConnector
import dev.pritam.host.tool.llmgateway.connector.LlmConnector
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import dev.pritam.host.tool.llmgateway.mcp.storage.McpServerRepository
import dev.pritam.host.tool.llmgateway.model.ChatCompletionRequest
import dev.pritam.host.tool.llmgateway.model.ChatCompletionResponse
import dev.pritam.host.tool.llmgateway.model.ChatMessage
import dev.pritam.host.tool.llmgateway.model.ChatStreamChunk
import dev.pritam.host.tool.llmgateway.model.LlmProfile
import dev.pritam.host.tool.llmgateway.model.ProfileStatus
import dev.pritam.host.tool.llmgateway.model.RouterResult
import dev.pritam.host.tool.llmgateway.storage.LlmProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

class LlmRouterEngine(
    private val repository: LlmProfileRepository,
    private val mcpRepository: McpServerRepository? = null
) {

    private val desktopConnector = DesktopLocalConnector()
    private val geminiConnector = GeminiCloudConnector()
    private val chatGptConnector = ChatGptCloudConnector()

    private fun getConnectorFor(profile: LlmProfile): LlmConnector {
        return when (profile.providerType) {
            "GEMINI_CLOUD", "GEMINI_PRO" -> geminiConnector
            "CHATGPT_CLOUD", "CHATGPT_FREE", "CUSTOM_OPENAI" -> chatGptConnector
            else -> desktopConnector
        }
    }

    suspend fun routeChat(rawRequest: ChatCompletionRequest): RouterResult<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        val allProfiles = repository.profiles.value
        val candidateProfiles = allProfiles.filter { it.isEnabled }.sortedBy { it.priorityOrder }

        if (candidateProfiles.isEmpty()) {
            AppLogHub.log(
                toolId = "llm-gateway",
                toolName = "LLM Gateway",
                level = LogLevel.ERROR,
                tag = "Routing",
                message = "LLM GATEWAY [ROUTING ERROR] No enabled LLM profiles configured in repository"
            )
            return@withContext RouterResult.AllTargetsExhausted(mapOf("error" to "No enabled profiles in gateway pool"))
        }

        // Auto-inject active MCP tools if none explicitly specified
        val effectiveTools = if (rawRequest.tools.isEmpty() && mcpRepository != null) {
            mcpRepository.getAllActiveTools()
        } else {
            rawRequest.tools
        }
        val request = rawRequest.copy(tools = effectiveTools)

        val attemptedProfiles = mutableListOf<String>()
        val errorMap = mutableMapOf<String, String>()

        for ((index, profile) in candidateProfiles.withIndex()) {
            attemptedProfiles.add(profile.name)
            AppLogHub.log(
                toolId = "llm-gateway",
                toolName = "LLM Gateway",
                level = LogLevel.INFO,
                tag = "Routing",
                message = "LLM GATEWAY [ROUTING] Attempting target #${index + 1}: '${profile.name}' (${profile.providerType})" +
                        if (effectiveTools.isNotEmpty()) " with ${effectiveTools.size} MCP tools" else ""
            )

            try {
                val connector = getConnectorFor(profile)
                val initialResponse = connector.executeChat(profile, request)

                // Handle automated Tool Calling loop
                val finalResponse = if (initialResponse.toolCalls.isNotEmpty() && request.autoExecuteTools && mcpRepository != null) {
                    executeToolLoop(profile, connector, request, initialResponse)
                } else {
                    initialResponse
                }

                // Mark profile as active/healthy
                repository.updateStatus(profile.id, ProfileStatus.ACTIVE, finalResponse.latencyMs)

                if (attemptedProfiles.size == 1) {
                    AppLogHub.log(
                        toolId = "llm-gateway",
                        toolName = "LLM Gateway",
                        level = LogLevel.INFO,
                        tag = "Success",
                        message = "LLM GATEWAY [RESPONSE] '${profile.name}' responded in ${finalResponse.latencyMs}ms (${finalResponse.content.length} chars)"
                    )
                    return@withContext RouterResult.Success(
                        data = finalResponse.copy(fallbackAttempted = emptyList()),
                        profileUsed = profile.name
                    )
                } else {
                    val fallbacks = attemptedProfiles.dropLast(1)
                    AppLogHub.log(
                        toolId = "llm-gateway",
                        toolName = "LLM Gateway",
                        level = LogLevel.WARN,
                        tag = "Failover",
                        message = "LLM GATEWAY [FAILOVER RECOVERY] Successfully recovered on fallback target '${profile.name}' after ${fallbacks.size} failed attempts: $fallbacks"
                    )
                    return@withContext RouterResult.FallbackSuccess(
                        data = finalResponse.copy(fallbackAttempted = fallbacks),
                        attemptedProfiles = fallbacks,
                        finalProfile = profile.name
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: e.javaClass.simpleName
                errorMap[profile.name] = errorMsg

                val isRateLimit = errorMsg.contains("429") || errorMsg.contains("Quota", ignoreCase = true)
                val isUnreachable = e is ConnectException || e is SocketTimeoutException || errorMsg.contains("Failed to connect", ignoreCase = true)

                val newStatus = when {
                    isRateLimit -> ProfileStatus.RATE_LIMITED
                    isUnreachable -> ProfileStatus.HOST_UNREACHABLE
                    errorMsg.contains("401") || errorMsg.contains("403") -> ProfileStatus.EXPIRED
                    else -> ProfileStatus.HOST_UNREACHABLE
                }

                repository.updateStatus(profile.id, newStatus)

                val nextProfile = candidateProfiles.getOrNull(index + 1)
                AppLogHub.log(
                    toolId = "llm-gateway",
                    toolName = "LLM Gateway",
                    level = LogLevel.WARN,
                    tag = "Failover",
                    message = "LLM GATEWAY [FAILOVER TRIGGER] Target '${profile.name}' failed with status $newStatus ($errorMsg)." +
                            if (nextProfile != null) " Falling back to '${nextProfile.name}'." else " No more fallback profiles available."
                )
            }
        }

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.ERROR,
            tag = "Exhausted",
            message = "LLM GATEWAY [ALL TARGETS EXHAUSTED] All ${candidateProfiles.size} profiles failed: $errorMap"
        )
        RouterResult.AllTargetsExhausted(errorMap)
    }

    private suspend fun executeToolLoop(
        profile: LlmProfile,
        connector: LlmConnector,
        originalRequest: ChatCompletionRequest,
        initialResponse: ChatCompletionResponse
    ): ChatCompletionResponse {
        val repo = mcpRepository ?: return initialResponse
        val toolCalls = initialResponse.toolCalls
        val toolResults = mutableListOf<McpToolResult>()

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.INFO,
            tag = "MCP",
            message = "LLM GATEWAY [TOOL CALL LOOP] Executing ${toolCalls.size} tool calls requested by model '${initialResponse.model}'"
        )

        val updatedMessages = originalRequest.messages.toMutableList()
        // Add assistant tool-call declaration
        updatedMessages.add(
            ChatMessage(
                role = "assistant",
                content = initialResponse.content,
                toolCalls = toolCalls
            )
        )

        // Execute each tool call
        for (call in toolCalls) {
            val result = repo.executeToolCall(call)
            toolResults.add(result)
            updatedMessages.add(
                ChatMessage(
                    role = "tool",
                    content = result.content,
                    toolCallId = call.id
                )
            )
        }

        // Send follow-up request with tool results to obtain the final answer
        val followUpRequest = originalRequest.copy(
            messages = updatedMessages,
            autoExecuteTools = false // prevent infinite loops
        )

        val followUpResponse = connector.executeChat(profile, followUpRequest)
        return followUpResponse.copy(
            toolCalls = toolCalls,
            toolResults = toolResults,
            latencyMs = initialResponse.latencyMs + followUpResponse.latencyMs
        )
    }

    fun routeChatStream(rawRequest: ChatCompletionRequest): Flow<RouterResult<ChatStreamChunk>> = flow {
        val allProfiles = repository.profiles.value
        val candidateProfiles = allProfiles.filter { it.isEnabled }.sortedBy { it.priorityOrder }

        if (candidateProfiles.isEmpty()) {
            emit(RouterResult.AllTargetsExhausted(mapOf("error" to "No enabled profiles in gateway pool")))
            return@flow
        }

        val effectiveTools = if (rawRequest.tools.isEmpty() && mcpRepository != null) {
            mcpRepository.getAllActiveTools()
        } else {
            rawRequest.tools
        }
        val request = rawRequest.copy(tools = effectiveTools)

        val attemptedProfiles = mutableListOf<String>()
        val errorMap = mutableMapOf<String, String>()

        for ((index, profile) in candidateProfiles.withIndex()) {
            attemptedProfiles.add(profile.name)
            val connector = getConnectorFor(profile)
            var hasEmittedAnyChunk = false

            try {
                connector.streamChat(profile, request)
                    .onEach { chunk ->
                        hasEmittedAnyChunk = true
                        val fallbacks = if (attemptedProfiles.size > 1) attemptedProfiles.dropLast(1) else emptyList()
                        val resultChunk = chunk.copy(fallbackAttempted = fallbacks)
                        if (fallbacks.isEmpty()) {
                            emit(RouterResult.Success(resultChunk, profile.name))
                        } else {
                            emit(RouterResult.FallbackSuccess(resultChunk, fallbacks, profile.name))
                        }
                    }
                    .collect {}

                repository.updateStatus(profile.id, ProfileStatus.ACTIVE)
                return@flow
            } catch (e: Exception) {
                val errorMsg = e.message ?: e.javaClass.simpleName
                errorMap[profile.name] = errorMsg

                val newStatus = when {
                    errorMsg.contains("429") -> ProfileStatus.RATE_LIMITED
                    e is ConnectException || e is SocketTimeoutException -> ProfileStatus.HOST_UNREACHABLE
                    else -> ProfileStatus.HOST_UNREACHABLE
                }
                repository.updateStatus(profile.id, newStatus)

                if (hasEmittedAnyChunk) {
                    AppLogHub.log(
                        toolId = "llm-gateway",
                        toolName = "LLM Gateway",
                        level = LogLevel.ERROR,
                        tag = "StreamError",
                        message = "LLM GATEWAY [STREAM MIDWAY ERROR] '${profile.name}' broke during streaming: $errorMsg"
                    )
                    emit(RouterResult.AllTargetsExhausted(mapOf(profile.name to "Stream aborted midway: $errorMsg")))
                    return@flow
                }

                val nextProfile = candidateProfiles.getOrNull(index + 1)
                AppLogHub.log(
                    toolId = "llm-gateway",
                    toolName = "LLM Gateway",
                    level = LogLevel.WARN,
                    tag = "StreamFailover",
                    message = "LLM GATEWAY [STREAM FAILOVER] '${profile.name}' connection failed before first chunk ($errorMsg)." +
                            if (nextProfile != null) " Falling back to '${nextProfile.name}'." else " All profiles exhausted."
                )
            }
        }

        emit(RouterResult.AllTargetsExhausted(errorMap))
    }.flowOn(Dispatchers.IO)

    suspend fun pingProfile(profile: LlmProfile): LlmProfile = withContext(Dispatchers.IO) {
        val connector = getConnectorFor(profile)
        val ping = connector.ping(profile)
        val newStatus = if (ping.isSuccess) ProfileStatus.ACTIVE else ProfileStatus.HOST_UNREACHABLE
        repository.updateStatus(profile.id, newStatus, ping.latencyMs)
        profile.copy(
            status = newStatus,
            latencyMs = ping.latencyMs,
            targetModel = if (profile.targetModel.isNullOrBlank() && ping.models.isNotEmpty()) ping.models.first() else profile.targetModel
        )
    }
}
