package dev.motherofallapps.host.tool.logviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.config.ToolRegistryConfig
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.logging.ToolLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LogFilterState(
    val selectedToolIds: Set<String> = emptySet(), // Empty means all tools
    val selectedLevels: Set<LogLevel> = emptySet(), // Empty means all levels
    val searchQuery: String = "",
    val isAutoScroll: Boolean = true,
)

data class LogViewerStats(
    val totalCount: Int = 0,
    val matchCount: Int = 0,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val debugCount: Int = 0,
    val infoCount: Int = 0,
)

class LogViewerViewModel : ViewModel() {

    private val _filterState = MutableStateFlow(LogFilterState())
    val filterState: StateFlow<LogFilterState> = _filterState.asStateFlow()

    val rawLogs: StateFlow<List<ToolLog>> = AppLogHub.logs

    val filteredLogs: StateFlow<List<ToolLog>> = combine(rawLogs, _filterState) { logs, filter ->
        logs.filter { log ->
            val matchesTool = filter.selectedToolIds.isEmpty() || filter.selectedToolIds.contains(log.toolId)
            val matchesLevel = filter.selectedLevels.isEmpty() || filter.selectedLevels.contains(log.level)
            val matchesSearch = filter.searchQuery.isBlank() ||
                    log.message.contains(filter.searchQuery, ignoreCase = true) ||
                    log.tag.contains(filter.searchQuery, ignoreCase = true) ||
                    log.toolName.contains(filter.searchQuery, ignoreCase = true)

            matchesTool && matchesLevel && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<LogViewerStats> = combine(rawLogs, filteredLogs) { all, filtered ->
        LogViewerStats(
            totalCount = all.size,
            matchCount = filtered.size,
            errorCount = all.count { it.level == LogLevel.ERROR },
            warningCount = all.count { it.level == LogLevel.WARN },
            debugCount = all.count { it.level == LogLevel.DEBUG },
            infoCount = all.count { it.level == LogLevel.INFO },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogViewerStats())

    fun toggleLevel(level: LogLevel) {
        val current = _filterState.value.selectedLevels
        val updated = if (current.contains(level)) current - level else current + level
        _filterState.value = _filterState.value.copy(selectedLevels = updated)
    }

    fun clearLevelFilters() {
        _filterState.value = _filterState.value.copy(selectedLevels = emptySet())
    }

    fun selectAllLevels() {
        _filterState.value = _filterState.value.copy(selectedLevels = LogLevel.entries.toSet())
    }

    fun toggleTool(toolId: String) {
        val current = _filterState.value.selectedToolIds
        val updated = if (current.contains(toolId)) current - toolId else current + toolId
        _filterState.value = _filterState.value.copy(selectedToolIds = updated)
    }

    fun clearToolFilters() {
        _filterState.value = _filterState.value.copy(selectedToolIds = emptySet())
    }

    fun getAvailableTools(): List<Pair<String, String>> {
        val list = mutableListOf(
            "host-system" to "System Core"
        )
        ToolRegistryConfig.INSTALLED_TOOLS.forEach {
            list.add(it.id to it.name)
        }
        AppLogHub.getKnownTools().forEach { toolId ->
            if (list.none { it.first == toolId }) {
                list.add(toolId to toolId.replaceFirstChar { it.uppercase() })
            }
        }
        return list
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun toggleAutoScroll() {
        _filterState.value = _filterState.value.copy(isAutoScroll = !_filterState.value.isAutoScroll)
    }

    fun clearLogs() {
        AppLogHub.clear()
    }

    fun exportFilteredLogsToString(): String {
        val list = filteredLogs.value
        return buildString {
            appendLine("=== MOTHER OF ALL APPS - DIAGNOSTIC LOG EXPORT ===")
            appendLine("Total Entries: ${list.size}")
            appendLine("==================================================")
            list.forEach { log ->
                appendLine("[${log.formattedDateTime}] [${log.level.name}] [${log.toolName} / ${log.tag}]: ${log.message}")
                if (!log.stackTrace.isNullOrBlank()) {
                    appendLine(log.stackTrace)
                }
            }
        }
    }
}
