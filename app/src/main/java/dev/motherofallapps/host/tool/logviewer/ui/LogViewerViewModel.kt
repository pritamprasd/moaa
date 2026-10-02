package dev.motherofallapps.host.tool.logviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val selectedToolId: String? = null,
    val selectedLevel: LogLevel? = null,
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
            val matchesTool = filter.selectedToolId == null || log.toolId == filter.selectedToolId
            val matchesLevel = filter.selectedLevel == null || log.level == filter.selectedLevel
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

    fun setToolFilter(toolId: String?) {
        _filterState.value = _filterState.value.copy(selectedToolId = toolId)
    }

    fun setLevelFilter(level: LogLevel?) {
        _filterState.value = _filterState.value.copy(selectedLevel = level)
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
