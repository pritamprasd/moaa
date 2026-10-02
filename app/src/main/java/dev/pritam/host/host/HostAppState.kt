package dev.pritam.host.host

import android.content.Context
import dev.pritam.dynamictools.storage.DynamicToolStorageManager
import dev.pritam.host.config.ToolRegistryConfig
import dev.pritam.pluginapi.ToolId
import dev.pritam.pluginapi.ToolInfo
import dev.pritam.pluginapi.ToolState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HostAppState(context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tools = MutableStateFlow<List<ToolInfo>>(
        ToolRegistryConfig.getRegisteredToolInfos()
    )
    val tools: StateFlow<List<ToolInfo>> = _tools.asStateFlow()

    private val _pendingTargetToolId = MutableStateFlow<String?>(null)
    val pendingTargetToolId: StateFlow<String?> = _pendingTargetToolId.asStateFlow()

    init {
        context?.let { ctx ->
            val dynamicStorage = DynamicToolStorageManager.getInstance(ctx)
            scope.launch {
                dynamicStorage.toolsFlow.collect { dynamicList ->
                    val staticTools = ToolRegistryConfig.getRegisteredToolInfos()
                    val dynamicToolInfos = dynamicList.map { bundle ->
                        ToolInfo(
                            id = ToolId("dynamic_${bundle.manifest.toolId}"),
                            name = bundle.manifest.displayName,
                            description = bundle.manifest.description,
                            version = bundle.manifest.version,
                            state = ToolState.INSTALLED
                        )
                    }
                    _tools.value = staticTools + dynamicToolInfos
                }
            }
        }
    }

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