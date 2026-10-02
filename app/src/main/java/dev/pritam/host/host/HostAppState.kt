package dev.pritam.host.host

import dev.pritam.host.config.ToolRegistryConfig
import dev.pritam.pluginapi.ToolInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HostAppState {
    private val _tools = MutableStateFlow<List<ToolInfo>>(
        ToolRegistryConfig.getRegisteredToolInfos()
    )
    val tools: StateFlow<List<ToolInfo>> = _tools.asStateFlow()

    private val _pendingTargetToolId = MutableStateFlow<String?>(null)
    val pendingTargetToolId: StateFlow<String?> = _pendingTargetToolId.asStateFlow()

    fun setPendingTargetToolId(toolId: String?) {
        _pendingTargetToolId.value = toolId
    }

    fun clearPendingTargetToolId() {
        _pendingTargetToolId.value = null
    }

    fun reloadTools() {
        _tools.value = ToolRegistryConfig.getRegisteredToolInfos()
    }
}