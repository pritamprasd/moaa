package dev.motherofallapps.host.ftp.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed real-time information about an active LAN client session.
 */
data class FtpClientSessionInfo(
    val sessionId: String,
    val ip: String,
    val port: Int,
    val connectedAtEpochMs: Long = System.currentTimeMillis(),
    val username: String? = null,
    val currentActivity: String = "Connected",
    val bytesUploaded: Long = 0L,
    val bytesDownloaded: Long = 0L,
) {
    val connectedDurationString: String
        get() {
            val seconds = ((System.currentTimeMillis() - connectedAtEpochMs) / 1000).coerceAtLeast(0)
            val mins = seconds / 60
            val secs = seconds % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }

    val formattedConnectTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(connectedAtEpochMs))
}

/**
 * Real-time traffic telemetry and active sessions tracking.
 */
data class FtpTelemetry(
    val activeClientsCount: Int = 0,
    val connectedClients: List<FtpClientSessionInfo> = emptyList(),
    val totalBytesUploaded: Long = 0L,
    val totalBytesDownloaded: Long = 0L,
    val currentUploadSpeedBps: Long = 0L,
    val currentDownloadSpeedBps: Long = 0L,
    val recentLogs: List<FtpLogEntry> = emptyList(),
) {
    fun formatBytes(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = ("KMGTPE")[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(unit, exp.toDouble()), pre)
    }

    fun formatSpeed(bps: Long): String {
        return "${formatBytes(bps)}/s"
    }
}
