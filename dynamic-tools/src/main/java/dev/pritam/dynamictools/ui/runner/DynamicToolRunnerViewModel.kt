package dev.pritam.dynamictools.ui.runner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.dynamictools.engine.DynamicToolLlmClient
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.storage.DynamicToolStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DynamicToolRunnerViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = DynamicToolStorageManager.getInstance(application)
    private val llmClient = DynamicToolLlmClient()

    private val _currentTool = MutableStateFlow<DynamicToolBundle?>(null)
    val currentTool: StateFlow<DynamicToolBundle?> = _currentTool.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    private val _isRefining = MutableStateFlow(false)
    val isRefining: StateFlow<Boolean> = _isRefining.asStateFlow()

    private val _refinementStatus = MutableStateFlow<String?>(null)
    val refinementStatus: StateFlow<String?> = _refinementStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadTool(toolId: String) {
        val bundle = storageManager.getTool(toolId)
        _currentTool.value = bundle
    }

    fun reload() {
        _reloadTrigger.value = _reloadTrigger.value + 1
    }

    fun updateCode(html: String, css: String, js: String) {
        val tool = _currentTool.value ?: return
        val success = storageManager.updateToolCode(tool.manifest.toolId, html, css, js)
        if (success) {
            _currentTool.value = storageManager.getTool(tool.manifest.toolId)
            reload()
        }
    }

    fun refineWithAi(refinementPrompt: String) {
        val tool = _currentTool.value ?: return
        if (refinementPrompt.isBlank() || _isRefining.value) return

        _isRefining.value = true
        _refinementStatus.value = "Iterating tool with LLM Gateway..."
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = llmClient.refineTool(tool, refinementPrompt)
                result.onSuccess { updatedBundle ->
                    storageManager.saveTool(updatedBundle)
                    _currentTool.value = storageManager.getTool(tool.manifest.toolId)
                    _isRefining.value = false
                    _refinementStatus.value = null
                    reload()
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

    fun clearError() {
        _errorMessage.value = null
    }
}
