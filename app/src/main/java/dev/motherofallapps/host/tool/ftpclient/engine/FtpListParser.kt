package dev.motherofallapps.host.tool.ftpclient.engine

import dev.motherofallapps.host.tool.ftpclient.model.FtpRemoteFile

/**
 * Robust FTP directory listing parser supporting Unix ls -l, MLSD (RFC 3659), and DOS listings.
 */
object FtpListParser {

    fun parseLine(rawLine: String, currentDir: String): FtpRemoteFile? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("total ") || trimmed == "." || trimmed == "..") {
            return null
        }

        // 1. Try MLSD RFC 3659 format: type=dir;size=123;modify=...; filename
        if (trimmed.startsWith("type=", ignoreCase = true) || trimmed.contains("; ")) {
            val mlsdParsed = parseMlsd(trimmed, currentDir)
            if (mlsdParsed != null) return mlsdParsed
        }

        // 2. Try standard UNIX ls -l format (e.g. "drwxr-xr-x  2 user group 4096 Oct 02 12:00 MyFolder")
        if (trimmed.length >= 10 && (trimmed[0] == '-' || trimmed[0] == 'd' || trimmed[0] == 'l' || trimmed[0] == 'c' || trimmed[0] == 'b' || trimmed[0] == 'p' || trimmed[0] == 's')) {
            val unixParsed = parseUnix(trimmed, currentDir)
            if (unixParsed != null) return unixParsed
        }

        // 3. Try Windows / DOS format (e.g. "01-02-23  04:50PM  <DIR>  FolderName")
        val dosParsed = parseDos(trimmed, currentDir)
        if (dosParsed != null) return dosParsed

        // Fallback: simple line as file
        val cleanName = trimmed.substringAfterLast('/').substringAfterLast('\\')
        val fullPath = buildFullPath(currentDir, cleanName)
        return FtpRemoteFile(
            name = cleanName,
            path = fullPath,
            isDirectory = false,
            sizeBytes = 0L,
            rawLine = rawLine
        )
    }

    private fun parseMlsd(line: String, currentDir: String): FtpRemoteFile? {
        try {
            val spaceIndex = line.indexOf(' ')
            if (spaceIndex < 0) return null

            val factsPart = line.substring(0, spaceIndex)
            val name = line.substring(spaceIndex + 1).trim()
            if (name.isEmpty() || name == "." || name == "..") return null

            var isDir = false
            var size = 0L
            var modify = ""

            factsPart.split(';').forEach { fact ->
                val eq = fact.indexOf('=')
                if (eq > 0) {
                    val key = fact.substring(0, eq).trim().lowercase()
                    val value = fact.substring(eq + 1).trim()
                    when (key) {
                        "type" -> isDir = value.equals("dir", ignoreCase = true) || value.equals("cdir", ignoreCase = true) || value.equals("pdir", ignoreCase = true)
                        "size" -> size = value.toLongOrNull() ?: 0L
                        "modify" -> modify = value
                    }
                }
            }

            return FtpRemoteFile(
                name = name,
                path = buildFullPath(currentDir, name),
                isDirectory = isDir,
                sizeBytes = size,
                lastModifiedFormatted = modify,
                rawLine = line
            )
        } catch (ignored: Exception) {
            return null
        }
    }

    private fun parseDos(line: String, currentDir: String): FtpRemoteFile? {
        // Sample: 01-02-23  04:50PM       <DIR>          MyFolder
        // Sample: 01-02-23  04:50PM             1048576  MyFile.txt
        val parts = line.split("\\s+".toRegex())
        if (parts.size >= 4 && parts[0].firstOrNull()?.isDigit() == true && (parts[0].contains('-') || parts[0].contains('/'))) {
            val isDir = parts[2].equals("<DIR>", ignoreCase = true)
            val size = if (isDir) 0L else parts[2].toLongOrNull() ?: 0L
            val name = parts.drop(3).joinToString(" ")
            if (name.isNotEmpty() && name != "." && name != "..") {
                return FtpRemoteFile(
                    name = name,
                    path = buildFullPath(currentDir, name),
                    isDirectory = isDir,
                    sizeBytes = size,
                    lastModifiedFormatted = "${parts[0]} ${parts[1]}",
                    rawLine = line
                )
            }
        }
        return null
    }

    private fun parseUnix(line: String, currentDir: String): FtpRemoteFile? {
        // Sample: drwxr-xr-x 2 user group 4096 Oct 02 12:00 my_folder
        // Sample: -rw-r--r-- 1 root root 1024 Oct 02 12:00 file.txt
        val isDir = line.startsWith("d")
        val parts = line.split("\\s+".toRegex())
        if (parts.size >= 9) {
            val permissions = parts[0]
            val size = parts[4].toLongOrNull() ?: 0L
            val dateStr = "${parts[5]} ${parts[6]} ${parts[7]}"
            val name = parts.drop(8).joinToString(" ")

            // Handle symlinks (e.g. "name -> target")
            val cleanName = if (name.contains(" -> ")) name.substringBefore(" -> ").trim() else name
            if (cleanName.isEmpty() || cleanName == "." || cleanName == "..") return null

            return FtpRemoteFile(
                name = cleanName,
                path = buildFullPath(currentDir, cleanName),
                isDirectory = isDir || permissions.startsWith("d"),
                sizeBytes = size,
                lastModifiedFormatted = dateStr,
                permissions = permissions,
                rawLine = line
            )
        }
        return null
    }

    fun buildFullPath(currentDir: String, fileName: String): String {
        val base = if (currentDir.endsWith("/")) currentDir else "$currentDir/"
        return (base + fileName).replace("//", "/")
    }
}
