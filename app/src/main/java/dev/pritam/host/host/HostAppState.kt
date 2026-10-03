package dev.pritam.host.host

import android.content.Context
import dev.pritam.dynamictools.storage.DynamicToolStorageManager
import dev.pritam.host.config.ToolRegistryConfig
import dev.pritam.host.settings.AppSettingsManager
import dev.pritam.pluginapi.ToolId
import dev.pritam.pluginapi.ToolInfo
import dev.pritam.pluginapi.ToolState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HostAppState(context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tools = MutableStateFlow<List<ToolInfo>>(
        applyOrdering(ToolRegistryConfig.getRegisteredToolInfos(), AppSettingsManager.toolOrder.value)
    )
    val tools: StateFlow<List<ToolInfo>> = _tools.asStateFlow()

    private val _pendingTargetToolId = MutableStateFlow<String?>(null)
    val pendingTargetToolId: StateFlow<String?> = _pendingTargetToolId.asStateFlow()

    init {
        context?.let { ctx ->
            AppSettingsManager.init(ctx)
            val dynamicStorage = DynamicToolStorageManager.getInstance(ctx)
            scope.launch {
                combine(dynamicStorage.toolsFlow, AppSettingsManager.toolOrder) { dynamicList, customOrder ->
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
                    val allTools = staticTools + dynamicToolInfos
                    applyOrdering(allTools, customOrder)
                }.collect { orderedTools ->
                    _tools.value = orderedTools
                }
            }
        } ?: run {
            _tools.value = applyOrdering(ToolRegistryConfig.getRegisteredToolInfos(), AppSettingsManager.toolOrder.value)
        }
    }

    fun setPendingTargetToolId(toolId: String?) {
        _pendingTargetToolId.value = toolId
    }

    fun clearPendingTargetToolId() {
        _pendingTargetToolId.value = null
    }

    fun reloadTools() {
        _tools.value = applyOrdering(ToolRegistryConfig.getRegisteredToolInfos(), AppSettingsManager.toolOrder.value)
    }

    fun reorderTools(fromIndex: Int, toIndex: Int) {
        val current = _tools.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _tools.value = current
            val idList = current.map { it.id.value }
            AppSettingsManager.setToolOrder(idList)
        }
    }

    fun updateToolOrder(newOrder: List<String>) {
        AppSettingsManager.setToolOrder(newOrder)
    }

    fun resetToolOrder() {
        AppSettingsManager.resetToolOrder()
    }

    companion object {
        fun applyOrdering(tools: List<ToolInfo>, order: List<String>): List<ToolInfo> {
            if (order.isEmpty()) return tools
            val orderMap = order.mapIndexed { index, id -> id to index }.toMap()
            return tools.sortedWith(
                compareBy<ToolInfo> { tool ->
                    orderMap[tool.id.value] ?: (1000 + tools.indexOf(tool))
                }
            )
        }
    }
}