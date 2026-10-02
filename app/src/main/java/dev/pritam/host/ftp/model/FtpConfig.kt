package dev.pritam.host.ftp.model

import android.os.Environment

/**
 * Configuration options for the local FTP server instance.
 *
 * @property port The TCP port to bind the FTP control socket (default: 2121).
 * @property username Mandatory username for client authentication.
 * @property password Mandatory password for client authentication.
 * @property rootPath The root local storage directory exposed to FTP clients.
 * @property isAnonymousAllowed Whether anonymous logins are permitted (always false by default).
 * @property maxClients Maximum concurrent client connections allowed.
 */
data class FtpConfig(
    val port: Int = DEFAULT_PORT,
    val username: String = DEFAULT_USERNAME,
    val password: String = DEFAULT_PASSWORD,
    val rootPath: String = DEFAULT_ROOT_PATH,
    val isAnonymousAllowed: Boolean = false,
    val maxClients: Int = 8,
) {
    companion object {
        const val DEFAULT_PORT = 2121
        const val DEFAULT_USERNAME = "user"
        const val DEFAULT_PASSWORD = "password123"
        val DEFAULT_ROOT_PATH: String = Environment.getExternalStorageDirectory().absolutePath
    }
}
