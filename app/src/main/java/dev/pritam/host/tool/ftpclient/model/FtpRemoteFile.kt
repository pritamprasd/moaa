package dev.pritam.host.tool.ftpclient.model

/**
 * Metadata for a remote file or folder in an FTP directory listing.
 */
data class FtpRemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModifiedFormatted: String = "",
    val permissions: String = "",
    val rawLine: String = "",
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> "%.2f GB".format(gb)
                mb >= 1.0 -> "%.2f MB".format(mb)
                kb >= 1.0 -> "%.1f KB".format(kb)
                else -> "$sizeBytes B"
            }
        }

    val fileExtension: String
        get() {
            if (isDirectory) return ""
            val idx = name.lastIndexOf('.')
            return if (idx >= 0 && idx < name.length - 1) name.substring(idx + 1).lowercase() else ""
        }
}
