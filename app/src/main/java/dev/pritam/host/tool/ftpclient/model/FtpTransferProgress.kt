package dev.pritam.host.tool.ftpclient.model

/**
 * Real-time telemetry for active file upload or download.
 */
data class FtpTransferProgress(
    val fileName: String,
    val remotePath: String,
    val isUpload: Boolean,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val isComplete: Boolean = false,
    val errorMsg: String? = null,
    val startTimeMs: Long = System.currentTimeMillis(),
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val formattedSpeed: String
        get() {
            val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000.0
            if (elapsedSec <= 0.1) return "Calculating..."
            val bytesPerSec = bytesTransferred / elapsedSec
            val kbPerSec = bytesPerSec / 1024.0
            val mbPerSec = kbPerSec / 1024.0
            return when {
                mbPerSec >= 1.0 -> "%.2f MB/s".format(mbPerSec)
                kbPerSec >= 1.0 -> "%.1f KB/s".format(kbPerSec)
                else -> "${bytesPerSec.toLong()} B/s"
            }
        }
}
