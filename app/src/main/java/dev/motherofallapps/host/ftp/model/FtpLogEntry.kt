package dev.motherofallapps.host.ftp.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FtpLogLevel {
    INFO,
    TRANSFER,
    AUTH,
    ERROR,
    WARNING,
}

data class FtpLogEntry(
    val timestampMs: Long = System.currentTimeMillis(),
    val level: FtpLogLevel = FtpLogLevel.INFO,
    val message: String,
    val clientIp: String? = null,
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
}
