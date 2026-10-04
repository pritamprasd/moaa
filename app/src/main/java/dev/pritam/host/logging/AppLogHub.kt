package dev.pritam.host.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global centralized observable log hub.
 * All current and future tools publish their diagnostic and debug logs here.
 */
object AppLogHub {

    private const val MAX_LOG_BUFFER_SIZE = 2000

    private val _logs = MutableStateFlow<List<ToolLog>>(emptyList())
    val logs: StateFlow<List<ToolLog>> = _logs.asStateFlow()

    private val registeredTools = mutableSetOf<String>()

    init {
        // Log initialization of log hub
        log(
            toolId = "host-system",
            toolName = "System Core",
            level = LogLevel.INFO,
            tag = "LogHub",
            message = "Central Diagnostic Log Hub initialized"
        )
    }

    fun log(
        toolId: String,
        toolName: String,
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, String> = emptyMap(),
    ) {
        registeredTools.add(toolId)

        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = ToolLog(
            toolId = toolId,
            toolName = toolName,
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace,
            metadata = metadata
        )

        // Mirror to Android Logcat
        when (level) {
            LogLevel.VERBOSE -> Log.v("[$toolId/$tag]", message, throwable)
            LogLevel.DEBUG -> Log.d("[$toolId/$tag]", message, throwable)
            LogLevel.INFO -> Log.i("[$toolId/$tag]", message, throwable)
            LogLevel.WARN -> Log.w("[$toolId/$tag]", message, throwable)
            LogLevel.ERROR -> Log.e("[$toolId/$tag]", message, throwable)
        }

        _logs.update { current ->
            val capacity = minOf(current.size + 1, MAX_LOG_BUFFER_SIZE)
            val updated = ArrayList<ToolLog>(capacity)
            updated.add(entry)
            val toCopy = minOf(current.size, MAX_LOG_BUFFER_SIZE - 1)
            for (i in 0 until toCopy) {
                updated.add(current[i])
            }
            updated
        }
    }

    /**
     * Instantly prunes in-memory logs down to [targetSize] to reclaim heap memory.
     * @return Number of purged log entries.
     */
    fun pruneDownTo(targetSize: Int): Int {
        var purgedCount = 0
        _logs.update { current ->
            if (current.size > targetSize) {
                purgedCount = current.size - targetSize
                current.take(targetSize)
            } else {
                current
            }
        }
        return purgedCount
    }

    /**
     * Explicitly prunes logs older than the given retention policy.
     * @return Number of purged log entries.
     */
    fun pruneExpiredLogs(policy: dev.pritam.host.settings.LogRetentionPolicy): Int {
        if (policy.durationMs == Long.MAX_VALUE) return 0
        val cutoff = System.currentTimeMillis() - policy.durationMs
        var purgedCount = 0
        _logs.update { current ->
            val filtered = current.filter { it.timestampMs >= cutoff }
            purgedCount = current.size - filtered.size
            filtered
        }
        return purgedCount
    }

    fun clear() {
        _logs.value = emptyList()
        log(
            toolId = "host-system",
            toolName = "System Core",
            level = LogLevel.INFO,
            tag = "LogHub",
            message = "Log buffer cleared by user"
        )
    }

    fun logClipboardOperation(
        toolId: String,
        toolName: String,
        operationType: String, // e.g. "COPY", "PASTE", "CUT"
        label: String,
        content: String
    ) {
        val preview = if (content.length > 80) content.take(77) + "..." else content
        log(
            toolId = toolId,
            toolName = toolName,
            level = LogLevel.INFO,
            tag = "Clipboard",
            message = "CLIPBOARD OPERATION [$operationType] '$label' (${content.length} chars): \"$preview\""
        )
    }

    /**
     * Creates a scoped logger instance for a specific tool.
     */
    fun createLogger(toolId: String, toolName: String): ToolLogger {
        registeredTools.add(toolId)
        return ToolLogger(toolId, toolName)
    }

    fun getKnownTools(): List<String> = registeredTools.toList()
}

/**
 * Scoped helper logger for a specific tool.
 */
class ToolLogger(
    val toolId: String,
    val toolName: String,
) {
    fun v(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        AppLogHub.log(toolId, toolName, LogLevel.VERBOSE, tag, message, metadata = metadata)
    }

    fun d(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        AppLogHub.log(toolId, toolName, LogLevel.DEBUG, tag, message, metadata = metadata)
    }

    fun i(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        AppLogHub.log(toolId, toolName, LogLevel.INFO, tag, message, metadata = metadata)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        AppLogHub.log(toolId, toolName, LogLevel.WARN, tag, message, throwable, metadata)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        AppLogHub.log(toolId, toolName, LogLevel.ERROR, tag, message, throwable, metadata)
    }
}
