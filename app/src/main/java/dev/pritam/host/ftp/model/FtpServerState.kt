package dev.pritam.host.ftp.model

/**
 * Represents the observable lifecycle state of the FTP server.
 */
sealed interface FtpServerState {
    data object Stopped : FtpServerState

    data object Starting : FtpServerState

    data class Running(
        val ipAddress: String,
        val port: Int,
        val rootPath: String,
        val username: String,
        val startedAtEpochMs: Long = System.currentTimeMillis(),
    ) : FtpServerState {
        val connectionUrl: String
            get() = "ftp://$ipAddress:$port"
    }

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : FtpServerState
}
