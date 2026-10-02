package dev.motherofallapps.host.tool.ftpclient.model

import java.util.UUID

/**
 * Saved FTP Server connection profile with credentials and connection flags.
 */
data class FtpConnectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = "",
    val defaultRemotePath: String = "/",
    val isPassiveMode: Boolean = true,
    val isAnonymous: Boolean = false,
    val lastConnectedMs: Long = 0L,
    val notes: String = "",
) {
    val displaySubtitle: String
        get() = "$host:$port ($username)"
}
