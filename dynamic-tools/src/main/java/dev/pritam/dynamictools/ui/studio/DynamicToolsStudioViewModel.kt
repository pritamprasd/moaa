package dev.pritam.dynamictools.ui.studio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.dynamictools.engine.DynamicToolLlmClient
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolPreset
import dev.pritam.dynamictools.storage.DynamicToolStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DynamicToolsStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = DynamicToolStorageManager.getInstance(application)
    private val llmClient = DynamicToolLlmClient()

    val installedTools: StateFlow<List<DynamicToolBundle>> = storageManager.toolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStatus = MutableStateFlow<String?>(null)
    val generationStatus: StateFlow<String?> = _generationStatus.asStateFlow()

    private val _isRefining = MutableStateFlow(false)
    val isRefining: StateFlow<Boolean> = _isRefining.asStateFlow()

    private val _refinementStatus = MutableStateFlow<String?>(null)
    val refinementStatus: StateFlow<String?> = _refinementStatus.asStateFlow()

    private val _lastGeneratedToolId = MutableStateFlow<String?>(null)
    val lastGeneratedToolId: StateFlow<String?> = _lastGeneratedToolId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setPrompt(text: String) {
        _promptInput.value = text
        _errorMessage.value = null
    }

    fun applyPreset(preset: DynamicToolPreset) {
        _promptInput.value = preset.prompt
        _errorMessage.value = null
    }

    fun generateTool(onSuccess: (String) -> Unit = {}) {
        val prompt = _promptInput.value.trim()
        if (prompt.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _generationStatus.value = "Connecting to LLM Gateway & synthesizing HTML/CSS/JS..."
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = llmClient.generateTool(prompt)
                result.onSuccess { bundle ->
                    _generationStatus.value = "Saving custom tool to sandboxed storage..."
                    storageManager.saveTool(bundle)
                    _lastGeneratedToolId.value = bundle.manifest.toolId
                    _isGenerating.value = false
                    _generationStatus.value = null
                    _promptInput.value = ""
                    onSuccess(bundle.manifest.toolId)
                }.onFailure { error ->
                    _isGenerating.value = false
                    _generationStatus.value = null
                    _errorMessage.value = error.message ?: "Failed to generate tool"
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _generationStatus.value = null
                _errorMessage.value = e.message ?: "Unexpected error during generation"
            }
        }
    }

    fun refineTool(tool: DynamicToolBundle, refinementPrompt: String, onSuccess: (String) -> Unit = {}) {
        val prompt = refinementPrompt.trim()
        if (prompt.isBlank() || _isRefining.value) return

        _isRefining.value = true
        _refinementStatus.value = "Refining '${tool.manifest.displayName}' with LLM Gateway..."
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = llmClient.refineTool(tool, prompt)
                result.onSuccess { updatedBundle ->
                    storageManager.saveTool(updatedBundle)
                    _isRefining.value = false
                    _refinementStatus.value = null
                    onSuccess(updatedBundle.manifest.toolId)
                }.onFailure { error ->
                    _isRefining.value = false
                    _refinementStatus.value = null
                    _errorMessage.value = error.message ?: "Failed to refine tool"
                }
            } catch (e: Exception) {
                _isRefining.value = false
                _refinementStatus.value = null
                _errorMessage.value = e.message ?: "Unexpected error during refinement"
            }
        }
    }

    fun deleteTool(toolId: String) {
        storageManager.deleteTool(toolId)
    }

    fun exportToolJson(toolId: String): String? {
        return storageManager.exportToolBundleJson(toolId)
    }

    fun importToolJson(jsonStr: String): Boolean {
        val result = storageManager.importToolFromJson(jsonStr)
        return result.isSuccess
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLastGenerated() {
        _lastGeneratedToolId.value = null
    }
}
