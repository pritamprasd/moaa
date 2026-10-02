package dev.pritam.host.tool.ftpclient.model

/**
 * Top-level state of the FTP Client tool.
 */
enum class FtpClientConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class FtpClientState(
    val status: FtpClientConnectionStatus = FtpClientConnectionStatus.DISCONNECTED,
    val connectedProfile: FtpConnectionProfile? = null,
    val currentRemotePath: String = "/",
    val remoteFiles: List<FtpRemoteFile> = emptyList(),
    val isRefreshingFiles: Boolean = false,
    val errorMessage: String? = null,
    val serverWelcomeMessage: String = "",
    val activeTransfer: FtpTransferProgress? = null,
    val transferHistory: List<FtpTransferProgress> = emptyList(),
    val savedProfiles: List<FtpConnectionProfile> = emptyList(),
    val searchQuery: String = "",
    val isSortByName: Boolean = true,
    val isSortAscending: Boolean = true,
) {
    val isConnected: Boolean
        get() = status == FtpClientConnectionStatus.CONNECTED

    val filteredFiles: List<FtpRemoteFile>
        get() {
            var list = remoteFiles
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.name.lowercase().contains(q) }
            }
            return if (isSortByName) {
                if (isSortAscending) {
                    list.sortedWith(compareBy<FtpRemoteFile> { !it.isDirectory }.thenBy { it.name.lowercase() })
                } else {
                    list.sortedWith(compareBy<FtpRemoteFile> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
                }
            } else {
                if (isSortAscending) {
                    list.sortedWith(compareBy<FtpRemoteFile> { !it.isDirectory }.thenBy { it.sizeBytes })
                } else {
                    list.sortedWith(compareBy<FtpRemoteFile> { !it.isDirectory }.thenByDescending { it.sizeBytes })
                }
            }
        }
}
