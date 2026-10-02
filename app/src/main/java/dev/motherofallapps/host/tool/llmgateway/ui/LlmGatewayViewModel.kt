package dev.motherofallapps.host.tool.llmgateway.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmgateway.manager.LlmGatewayManager
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionResponse
import dev.motherofallapps.host.tool.llmgateway.model.ChatMessage
import dev.motherofallapps.host.tool.llmgateway.model.DiscoveredHost
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
import dev.motherofallapps.host.tool.llmgateway.model.ProfileStatus
import dev.motherofallapps.host.tool.llmgateway.model.ProviderCategory
import dev.motherofallapps.host.tool.llmgateway.model.RouterResult
import dev.motherofallapps.host.tool.llmgateway.server.GatewayServerTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LlmGatewayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LlmGatewayManager.getRepository(application)
    private val routerEngine = LlmGatewayManager.getRouterEngine(application)
    private val scanner = LlmGatewayManager.getScanner(application)
    private val httpServer = LlmGatewayManager.getHttpServer(application)

    val profiles: StateFlow<List<LlmProfile>> = repository.profiles
    val serverTelemetry: StateFlow<GatewayServerTelemetry> = httpServer.telemetry
    val isScanning: StateFlow<Boolean> = scanner.isScanning
    val scanProgress: StateFlow<Float> = scanner.scanProgress
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = scanner.discoveredHosts

    private val _selectedTab = MutableStateFlow(0) // 0 = Accounts & Hosts, 1 = Routing Rules, 2 = Status & Live Test
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Live Test Sandbox State
    private val _promptInput = MutableStateFlow("Explain how quantum computing works in 2 short sentences.")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastResult = MutableStateFlow<RouterResult<ChatCompletionResponse>?>(null)
    val lastResult: StateFlow<RouterResult<ChatCompletionResponse>?> = _lastResult.asStateFlow()

    private val _pingingProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val pingingProfileIds: StateFlow<Set<String>> = _pingingProfileIds.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPromptInput(text: String) {
        _promptInput.value = text
    }

    fun toggleServer() {
        if (serverTelemetry.value.isRunning) {
            httpServer.stop()
        } else {
            httpServer.start()
        }
    }

    fun startLanDiscovery() {
        viewModelScope.launch {
            scanner.scanLocalSubnet()
        }
    }

    fun addDiscoveredHostAsProfile(host: DiscoveredHost) {
        val defaultModel = host.modelsAvailable.firstOrNull() ?: if (host.serviceType == "Ollama") "llama3.2:latest" else "local-model"
        val profile = LlmProfile(
            id = "profile-${UUID.randomUUID()}",
            name = "${host.serviceType} (${host.hostIp})",
            category = ProviderCategory.DESKTOP_LOCAL_HOST,
            providerType = if (host.serviceType == "Ollama") "OLLAMA_LOCAL" else "LM_STUDIO",
            hostAddress = host.fullUrl,
            targetModel = defaultModel,
            status = ProfileStatus.ACTIVE,
            latencyMs = host.responseTimeMs,
            isEnabled = true
        )
        repository.addProfile(profile)
        Toast.makeText(getApplication(), "Added '${profile.name}' to LLM Gateway pool", Toast.LENGTH_SHORT).show()
    }

    fun addCustomProfile(profile: LlmProfile) {
        repository.addProfile(profile)
        Toast.makeText(getApplication(), "Added '${profile.name}'", Toast.LENGTH_SHORT).show()
    }

    fun updateProfile(profile: LlmProfile) {
        repository.updateProfile(profile)
        Toast.makeText(getApplication(), "Updated '${profile.name}'", Toast.LENGTH_SHORT).show()
    }

    fun deleteProfile(profileId: String) {
        repository.deleteProfile(profileId)
        Toast.makeText(getApplication(), "Profile deleted", Toast.LENGTH_SHORT).show()
    }

    fun toggleProfileEnabled(profile: LlmProfile) {
        repository.updateProfile(profile.copy(isEnabled = !profile.isEnabled))
    }

    fun movePriority(fromIndex: Int, toIndex: Int) {
        repository.movePriority(fromIndex, toIndex)
    }

    fun pingProfile(profile: LlmProfile) {
        viewModelScope.launch {
            _pingingProfileIds.value = _pingingProfileIds.value + profile.id
            val updated = routerEngine.pingProfile(profile)
            _pingingProfileIds.value = _pingingProfileIds.value - profile.id

            val statusMsg = if (updated.status == ProfileStatus.ACTIVE) {
                "${profile.name}: Online (${updated.latencyMs}ms)"
            } else {
                "${profile.name}: Host Unreachable"
            }
            Toast.makeText(getApplication(), statusMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun pingAllProfiles() {
        viewModelScope.launch {
            val list = profiles.value
            list.forEach { p ->
                launch {
                    _pingingProfileIds.value = _pingingProfileIds.value + p.id
                    routerEngine.pingProfile(p)
                    _pingingProfileIds.value = _pingingProfileIds.value - p.id
                }
            }
        }
    }

    fun sendTestPrompt() {
        val query = _promptInput.value.trim()
        if (query.isEmpty() || _isGenerating.value) return

        val userMsg = ChatMessage(role = "user", content = query)
        val history = _chatMessages.value + userMsg
        _chatMessages.value = history
        _isGenerating.value = true
        _lastResult.value = null

        viewModelScope.launch {
            val request = ChatCompletionRequest(
                messages = history,
                stream = false
            )

            val result = routerEngine.routeChat(request)
            _lastResult.value = result
            _isGenerating.value = false

            when (result) {
                is RouterResult.Success -> {
                    val assistantMsg = ChatMessage(role = "assistant", content = result.data.content)
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }
                is RouterResult.FallbackSuccess -> {
                    val assistantMsg = ChatMessage(role = "assistant", content = result.data.content)
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }
                is RouterResult.AllTargetsExhausted -> {
                    val errMsg = ChatMessage(
                        role = "assistant",
                        content = "⚠️ [ALL GATEWAY TARGETS FAILED]\n${result.errors.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }}"
                    )
                    _chatMessages.value = _chatMessages.value + errMsg
                }
            }
        }
    }

    fun clearTestChat() {
        _chatMessages.value = emptyList()
        _lastResult.value = null
    }
}
