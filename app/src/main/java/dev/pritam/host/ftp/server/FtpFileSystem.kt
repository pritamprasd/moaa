package dev.pritam.host.ftp.server

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles virtual-to-physical path resolution, sandboxing, and directory listing formatting.
 */
class FtpFileSystem(val rootDirectory: File) {

    init {
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs()
        }
    }

    /**
     * Resolves a virtual FTP path relative to the rootDirectory.
     * Prevents path traversal attacks escaping rootDirectory.
     */
    fun resolve(currentVirtualDir: String, requestedPath: String): File? {
        val normalizedVirtual = when {
            requestedPath.startsWith("/") -> normalizeVirtualPath(requestedPath)
            else -> normalizeVirtualPath("$currentVirtualDir/$requestedPath")
        }

        val targetFile = if (normalizedVirtual == "/") {
            rootDirectory
        } else {
            File(rootDirectory, normalizedVirtual.removePrefix("/"))
        }

        return try {
            val canonicalTarget = targetFile.canonicalPath
            val canonicalRoot = rootDirectory.canonicalPath

            // Security check: Must be inside or equal to canonicalRoot
            if (canonicalTarget == canonicalRoot || canonicalTarget.startsWith("$canonicalRoot/")) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a physical file back to its virtual FTP path representation.
     */
    fun toVirtualPath(file: File): String {
        val canonicalTarget = file.canonicalPath
        val canonicalRoot = rootDirectory.canonicalPath

        if (canonicalTarget == canonicalRoot) return "/"
        if (canonicalTarget.startsWith("$canonicalRoot/")) {
            return "/" + canonicalTarget.removePrefix("$canonicalRoot/").replace('\\', '/')
        }
        return "/"
    }

    /**
     * Formats directory contents in RFC 959 Unix-style `ls -l` format for LIST command.
     */
    fun formatUnixListing(file: File): String {
        val isDir = file.isDirectory
        val permissions = if (isDir) "drwxr-xr-x" else "-rw-r--r--"
        val links = if (isDir) "3" else "1"
        val owner = "owner"
        val group = "group"
        val size = if (isDir) 4096L else file.length()

        val lastModified = file.lastModified()
        val now = System.currentTimeMillis()
        val sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000)

        val dateFormat = if (lastModified > sixMonthsAgo && lastModified <= now) {
            SimpleDateFormat("MMM dd HH:mm", Locale.US)
        } else {
            SimpleDateFormat("MMM dd  yyyy", Locale.US)
        }
        val dateStr = dateFormat.format(Date(lastModified))

        return String.format(
            Locale.US,
            "%s %3s %-8s %-8s %10d %s %s\r\n",
            permissions,
            links,
            owner,
            group,
            size,
            dateStr,
            file.name
        )
    }

    /**
     * Formats file info for RFC 3659 MLSD / MLST command.
     */
    fun formatMlsdEntry(file: File): String {
        val type = if (file.isDirectory) "dir" else "file"
        val size = if (file.isDirectory) "" else "size=${file.length()};"
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        val modify = "modify=${sdf.format(Date(file.lastModified()))};"
        val perm = if (file.isDirectory) "perm=flcdmpe;" else "perm=adfrw;"

        return "type=$type;${size}${modify}${perm} ${file.name}\r\n"
    }

    companion object {
        fun normalizeVirtualPath(path: String): String {
            val parts = path.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
            val stack = mutableListOf<String>()

            for (part in parts) {
                if (part == "..") {
                    if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                } else {
                    stack.add(part)
                }
            }

            return "/" + stack.joinToString("/")
        }
    }
}
