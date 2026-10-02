package dev.motherofallapps.host.tool.llmchat.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmchat.model.ChatMessage
import dev.motherofallapps.host.tool.llmchat.model.ChatSession
import dev.motherofallapps.host.tool.llmchat.model.PersonaPreset
import dev.motherofallapps.host.tool.llmchat.storage.ChatSessionRepository
import dev.motherofallapps.host.tool.llmgateway.manager.LlmGatewayManager
import dev.motherofallapps.host.tool.llmgateway.model.ChatMessage as GatewayChatMessage
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
import dev.motherofallapps.host.tool.llmgateway.model.RouterResult
import dev.motherofallapps.host.tool.llmgateway.server.GatewayServerTelemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LlmChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatSessionRepository(application)
    private val gatewayRouter = LlmGatewayManager.getRouterEngine(application)
    private val gatewayServer = LlmGatewayManager.getHttpServer(application)
    private val gatewayProfileRepo = LlmGatewayManager.getRepository(application)
    private val gatewayMcpRepo = LlmGatewayManager.getMcpRepository(application)

    val sessions: StateFlow<List<ChatSession>> = repository.sessions

    val activeSession: StateFlow<ChatSession?> = combine(
        repository.sessions,
        repository.activeSessionId
    ) { sessionList, activeId ->
        sessionList.firstOrNull { it.id == activeId } ?: sessionList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val serverTelemetry: StateFlow<GatewayServerTelemetry> = gatewayServer.telemetry
    val gatewayProfiles: StateFlow<List<LlmProfile>> = gatewayProfileRepo.profiles
    val mcpServers: StateFlow<List<dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerProfile>> = gatewayMcpRepo.servers

    private val _inputPrompt = MutableStateFlow("")
    val inputPrompt: StateFlow<String> = _inputPrompt.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var activeGenerationJob: Job? = null

    fun setInputPrompt(value: String) {
        _inputPrompt.value = value
    }

    fun selectSession(sessionId: String) {
        if (_isGenerating.value) stopGeneration()
        repository.selectSession(sessionId)
    }

    fun createNewSession(preset: PersonaPreset = PersonaPreset.DEFAULT_ASSISTANT) {
        if (_isGenerating.value) stopGeneration()
        repository.createNewSession(preset)
    }

    fun deleteSession(sessionId: String) {
        if (_isGenerating.value && activeSession.value?.id == sessionId) {
            stopGeneration()
        }
        repository.deleteSession(sessionId)
    }

    fun renameSession(sessionId: String, newTitle: String) {
        repository.renameSession(sessionId, newTitle)
    }

    fun updateSessionPersona(preset: PersonaPreset) {
        val current = activeSession.value ?: return
        repository.updateSessionSettings(
            sessionId = current.id,
            personaId = preset.id,
            systemPrompt = preset.systemPrompt,
            temperature = preset.defaultTemperature,
            targetModelOverride = current.targetModelOverride
        )
    }

    fun updateSessionSettings(systemPrompt: String, temperature: Float, targetModelOverride: String?) {
        val current = activeSession.value ?: return
        repository.updateSessionSettings(
            sessionId = current.id,
            personaId = current.personaId,
            systemPrompt = systemPrompt,
            temperature = temperature,
            targetModelOverride = targetModelOverride
        )
    }

    fun clearCurrentChat() {
        val current = activeSession.value ?: return
        repository.clearMessages(current.id)
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isGenerating.value = false
    }

    fun sendMessage() {
        val promptText = _inputPrompt.value.trim()
        if (promptText.isBlank() || _isGenerating.value) return

        val currentSession = activeSession.value ?: repository.createNewSession()
        val sessionId = currentSession.id

        _inputPrompt.value = ""
        _isGenerating.value = true

        val userMessage = ChatMessage(
            id = "msg-${UUID.randomUUID()}",
            role = "user",
            content = promptText,
            timestamp = System.currentTimeMillis()
        )
        repository.addMessage(sessionId, userMessage)

        AppLogHub.log(
            toolId = "llm-chat",
            toolName = "LLM Chat",
            level = LogLevel.INFO,
            tag = "USER_PROMPT",
            message = "LLM CHAT [USER] Prompt: \"${promptText.take(60)}\"..."
        )

        val assistantMessageId = "msg-${UUID.randomUUID()}"
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )
        repository.addMessage(sessionId, initialAssistantMessage)

        activeGenerationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val requestMessages = mutableListOf<GatewayChatMessage>()

            // 1. System Prompt
            if (currentSession.systemPrompt.isNotBlank()) {
                requestMessages.add(GatewayChatMessage(role = "system", content = currentSession.systemPrompt))
            }

            // 2. Multi-turn history (last 12 messages for token economy)
            val history = currentSession.messages.takeLast(12)
            history.forEach { msg ->
                if (msg.content.isNotBlank() && !msg.isError) {
                    requestMessages.add(GatewayChatMessage(role = msg.role, content = msg.content))
                }
            }
            requestMessages.add(GatewayChatMessage(role = "user", content = promptText))

            val request = ChatCompletionRequest(
                model = currentSession.targetModelOverride ?: "default",
                messages = requestMessages,
                temperature = currentSession.temperature.toDouble(),
                stream = true
            )

            val accumulatedContent = StringBuilder()
            var activeModel: String? = null
            var finalFailoverTrail: List<String> = emptyList()

            try {
                gatewayRouter.routeChatStream(request).collect { result ->
                    when (result) {
                        is RouterResult.Success -> {
                            val chunk = result.data
                            accumulatedContent.append(chunk.deltaContent)
                            activeModel = chunk.model.ifBlank { result.profileUsed }
                            repository.updateMessage(sessionId, assistantMessageId) { msg ->
                                msg.copy(
                                    content = accumulatedContent.toString(),
                                    modelUsed = activeModel,
                                    providerUsed = result.profileUsed,
                                    latencyMs = System.currentTimeMillis() - startTime,
                                    isStreaming = true
                                )
                            }
                        }

                        is RouterResult.FallbackSuccess -> {
                            val chunk = result.data
                            accumulatedContent.append(chunk.deltaContent)
                            activeModel = chunk.model.ifBlank { result.finalProfile }
                            finalFailoverTrail = result.attemptedProfiles
                            repository.updateMessage(sessionId, assistantMessageId) { msg ->
                                msg.copy(
                                    content = accumulatedContent.toString(),
                                    modelUsed = activeModel,
                                    providerUsed = result.finalProfile,
                                    failoverTrail = finalFailoverTrail,
                                    latencyMs = System.currentTimeMillis() - startTime,
                                    isStreaming = true
                                )
                            }
                        }

                        is RouterResult.AllTargetsExhausted -> {
                            val errText = "⚠️ Gateway Error: All available LLM profiles exhausted.\nDetails: ${result.errors}"
                            repository.updateMessage(sessionId, assistantMessageId) { msg ->
                                msg.copy(
                                    content = errText,
                                    isStreaming = false,
                                    isError = true,
                                    latencyMs = System.currentTimeMillis() - startTime
                                )
                            }
                            AppLogHub.log(
                                toolId = "llm-chat",
                                toolName = "LLM Chat",
                                level = LogLevel.ERROR,
                                tag = "ROUTING_ERROR",
                                message = "LLM CHAT [ERROR] Targets exhausted: ${result.errors}"
                            )
                        }
                    }
                }

                // Finalize streaming
                val elapsed = System.currentTimeMillis() - startTime
                repository.updateMessage(sessionId, assistantMessageId) { msg ->
                    msg.copy(
                        isStreaming = false,
                        latencyMs = elapsed
                    )
                }

                AppLogHub.log(
                    toolId = "llm-chat",
                    toolName = "LLM Chat",
                    level = LogLevel.INFO,
                    tag = "ASSISTANT_RESPONSE",
                    message = "LLM CHAT [RESPONSE] Completed in ${elapsed}ms (${accumulatedContent.length} chars) via [${activeModel ?: "Gateway"}]"
                )
            } catch (e: Exception) {
                val errText = "⚠️ Communication Error: ${e.message ?: "Failed to receive response from LLM Gateway"}"
                repository.updateMessage(sessionId, assistantMessageId) { msg ->
                    msg.copy(
                        content = errText,
                        isStreaming = false,
                        isError = true,
                        latencyMs = System.currentTimeMillis() - startTime
                    )
                }
            } finally {
                _isGenerating.value = false
                activeGenerationJob = null
            }
        }
    }

    fun regenerateLastResponse() {
        val current = activeSession.value ?: return
        if (current.messages.isEmpty() || _isGenerating.value) return

        val lastUserMsg = current.messages.lastOrNull { it.role == "user" } ?: return
        _inputPrompt.value = lastUserMsg.content
        sendMessage()
    }

    fun exportChatAsMarkdown(context: Context) {
        val current = activeSession.value ?: return
        val sb = StringBuilder()
        sb.append("# ${current.title}\n")
        sb.append("System Persona: ${current.personaId} (Temp: ${current.temperature})\n\n")

        current.messages.forEach { msg ->
            sb.append("### ${msg.role.uppercase()} (${msg.modelUsed ?: "Local"})\n")
            sb.append("${msg.content}\n\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Export - ${current.title}", sb.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Chat exported to clipboard as Markdown", Toast.LENGTH_SHORT).show()

        AppLogHub.log(
            toolId = "llm-chat",
            toolName = "LLM Chat",
            level = LogLevel.INFO,
            tag = "EXPORT",
            message = "LLM CHAT [EXPORT] Exported session '${current.title}' (${current.messages.size} messages) to clipboard."
        )
    }
}
