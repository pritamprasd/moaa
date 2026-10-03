package dev.pritam.host.tool.sysinfo.model

import androidx.compose.ui.graphics.Color

enum class RefreshInterval(val displayName: String, val intervalMs: Long) {
    ONE_SEC("1s (Real-time)", 1000L),
    THREE_SEC("3s", 3000L),
    FIVE_SEC("5s (Balanced)", 5000L),
    TEN_SEC("10s", 10000L),
    ONE_MIN("1min", 60000L),
    MANUAL("Manual / Paused", 0L)
}

data class SysInfoItem(
    val key: String,
    val value: String,
    val subtitle: String? = null,
    val isHighlighted: Boolean = false,
    val progressFraction: Float? = null, // Optional 0f..1f for RAM / Storage / Battery progress bars
    val progressColor: Color? = null
)

data class SysInfoGroup(
    val id: String,
    val title: String,
    val emoji: String,
    val accentColor: Color,
    val items: List<SysInfoItem>,
    val isExpanded: Boolean = true
) {
    fun toFormattedString(): String {
        val builder = StringBuilder()
        builder.append("=== $title ===\n")
        items.forEach { item ->
            builder.append("${item.key}: ${item.value}\n")
        }
        return builder.toString().trimEnd()
    }
}

data class CompleteSystemReport(
    val timestamp: Long = System.currentTimeMillis(),
    val groups: List<SysInfoGroup>
) {
    fun toFullText(): String {
        val builder = StringBuilder()
        builder.append("============================================================\n")
        builder.append("  MOTHER OF ALL APPS · FULL SYSTEM SPECS & TELEMETRY\n")
        builder.append("============================================================\n\n")
        groups.forEach { group ->
            builder.append(group.toFormattedString())
            builder.append("\n\n")
        }
        return builder.toString().trimEnd()
    }
}
