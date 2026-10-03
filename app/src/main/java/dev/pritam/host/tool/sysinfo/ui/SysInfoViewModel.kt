package dev.pritam.host.tool.sysinfo.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.host.tool.sysinfo.manager.SysInfoCollector
import dev.pritam.host.tool.sysinfo.model.CompleteSystemReport
import dev.pritam.host.tool.sysinfo.model.RefreshInterval
import dev.pritam.host.tool.sysinfo.model.SysInfoGroup
import dev.pritam.host.tool.sysinfo.model.SysInfoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SysInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val collector = SysInfoCollector(application)
    private var tickerJob: Job? = null

    private val _report = MutableStateFlow(collector.collectAll())
    val report: StateFlow<CompleteSystemReport> = _report.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshInterval = MutableStateFlow(RefreshInterval.FIVE_SEC)
    val refreshInterval: StateFlow<RefreshInterval> = _refreshInterval.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _collapsedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val collapsedGroupIds: StateFlow<Set<String>> = _collapsedGroupIds.asStateFlow()

    init {
        startTicker()
    }

    fun setRefreshInterval(interval: RefreshInterval) {
        _refreshInterval.value = interval
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        val interval = _refreshInterval.value
        if (interval == RefreshInterval.MANUAL || interval.intervalMs <= 0) return

        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(interval.intervalMs)
                refreshNow(silent = true)
            }
        }
    }

    fun refreshNow(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isRefreshing.value = true
            _report.value = collector.collectAll()
            if (!silent) _isRefreshing.value = false
        }
    }

    fun toggleGroupExpansion(groupId: String) {
        val current = _collapsedGroupIds.value
        _collapsedGroupIds.value = if (current.contains(groupId)) {
            current - groupId
        } else {
            current + groupId
        }
    }

    fun expandAll() {
        _collapsedGroupIds.value = emptySet()
    }

    fun collapseAll() {
        _collapsedGroupIds.value = _report.value.groups.map { it.id }.toSet()
    }

    fun copyItem(context: Context, item: SysInfoItem) {
        val text = "${item.key}: ${item.value}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(item.key, item.value))
        Toast.makeText(context, "Copied '${item.key}': ${item.value}", Toast.LENGTH_SHORT).show()
    }

    fun copyGroup(context: Context, group: SysInfoGroup) {
        val text = group.toFormattedString()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(group.title, text))
        Toast.makeText(context, "Copied entire group: ${group.title}", Toast.LENGTH_SHORT).show()
    }

    fun copyFullReport(context: Context) {
        val text = _report.value.toFullText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Complete System Info Report", text))
        Toast.makeText(context, "Complete System Report copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
