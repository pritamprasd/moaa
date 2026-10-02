package dev.motherofallapps.host.tool.ftpclient.engine

import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.ftpclient.model.FtpConnectionProfile
import dev.motherofallapps.host.tool.ftpclient.model.FtpRemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * High-performance, RFC 959 compliant asynchronous FTP client.
 */
class FtpClient {

    private var controlSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private var currentProfile: FtpConnectionProfile? = null
    var currentWorkingDirectory: String = "/"
        private set

    var serverGreeting: String = ""
        private set

    val isConnected: Boolean
        get() = controlSocket?.isConnected == true && controlSocket?.isClosed == false

    suspend fun connect(profile: FtpConnectionProfile): Result<String> = withContext(Dispatchers.IO) {
        disconnect()
        try {
            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.INFO,
                tag = "ClientSession",
                message = "FTP CLIENT [CONNECT] Connecting to ${profile.host}:${profile.port} (Profile: '${profile.name}')..."
            )

            val socket = Socket()
            socket.connect(InetSocketAddress(profile.host, profile.port), 12000)
            socket.soTimeout = 15000

            controlSocket = socket
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

            val welcomeResp = readResponse()
            serverGreeting = welcomeResp.message
            if (welcomeResp.code !in 200..299) {
                disconnect()
                return@withContext Result.failure(IllegalStateException("Server rejected connection: ${welcomeResp.raw}"))
            }

            // Authentication
            val user = if (profile.isAnonymous) "anonymous" else profile.username
            val pass = if (profile.isAnonymous) "anonymous@" else profile.password

            val userResp = sendCommand("USER $user")
            if (userResp.code == 331) {
                val passResp = sendCommand("PASS $pass")
                if (passResp.code !in 200..299) {
                    disconnect()
                    AppLogHub.log(
                        toolId = "ftp-client",
                        toolName = "FTP Client",
                        level = LogLevel.ERROR,
                        tag = "ClientSession",
                        message = "FTP CLIENT [AUTH FAILED] Invalid password for user '$user' on ${profile.host}"
                    )
                    return@withContext Result.failure(IllegalStateException("Authentication failed: ${passResp.message}"))
                }
            } else if (userResp.code !in 200..299) {
                disconnect()
                return@withContext Result.failure(IllegalStateException("User rejected: ${userResp.message}"))
            }

            // Set Binary Mode
            sendCommand("TYPE I")

            // Query working directory
            val pwdResp = sendCommand("PWD")
            currentWorkingDirectory = parsePwdPath(pwdResp.message) ?: profile.defaultRemotePath.ifBlank { "/" }

            // If profile specifies a default path, try navigating there
            if (profile.defaultRemotePath.isNotBlank() && profile.defaultRemotePath != "/") {
                val cwdResp = sendCommand("CWD ${profile.defaultRemotePath}")
                if (cwdResp.code in 200..299) {
                    currentWorkingDirectory = profile.defaultRemotePath
                }
            }

            currentProfile = profile

            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.INFO,
                tag = "ClientSession",
                message = "FTP CLIENT [CONNECT] Successfully connected to ${profile.host}:${profile.port} as '$user'. CWD: '$currentWorkingDirectory'"
            )

            Result.success("Connected to ${profile.host}")
        } catch (e: Exception) {
            disconnect()
            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.ERROR,
                tag = "ClientSession",
                message = "FTP CLIENT [CONNECT ERROR] Failed to connect to ${profile.host}:${profile.port}: ${e.message}",
                throwable = e
            )
            Result.failure(e)
        }
    }

    suspend fun listFiles(path: String = currentWorkingDirectory): Result<List<FtpRemoteFile>> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))

        try {
            // First try MLSD, then fallback to LIST
            val dataSocket = openPassiveDataSocket()
                ?: return@withContext Result.failure(IllegalStateException("Failed to open passive data connection (PASV)"))

            val resp = sendCommand("MLSD $path")
            val isMlsdSupported = resp.code in 100..199

            val activeDataSocket = if (!isMlsdSupported) {
                // Try standard LIST
                try { dataSocket.close() } catch (ignored: Exception) {}
                val fallbackDataSocket = openPassiveDataSocket()
                    ?: return@withContext Result.failure(IllegalStateException("Failed to open passive data connection for LIST"))
                val listResp = sendCommand("LIST $path")
                if (listResp.code !in 100..199) {
                    try { fallbackDataSocket.close() } catch (ignored: Exception) {}
                    return@withContext Result.failure(IllegalStateException("Listing failed: ${listResp.message}"))
                }
                fallbackDataSocket
            } else {
                dataSocket
            }

            val dataReader = BufferedReader(InputStreamReader(activeDataSocket.getInputStream(), StandardCharsets.UTF_8))
            val files = mutableListOf<FtpRemoteFile>()
            var line: String?

            while (dataReader.readLine().also { line = it } != null) {
                val nonNullLine = line ?: break
                val parsed = FtpListParser.parseLine(nonNullLine, path)
                if (parsed != null) {
                    files.add(parsed)
                }
            }

            try { activeDataSocket.close() } catch (ignored: Exception) {}
            val transferDoneResp = readResponse()

            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.DEBUG,
                tag = "ClientBrowser",
                message = "FTP CLIENT [LIST] Listed ${files.size} items in '$path'"
            )

            Result.success(files)
        } catch (e: Exception) {
            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.WARN,
                tag = "ClientBrowser",
                message = "FTP CLIENT [LIST ERROR] Error listing '$path': ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun changeDirectory(targetPath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))

        try {
            val resp = sendCommand("CWD $targetPath")
            if (resp.code in 200..299) {
                val pwdResp = sendCommand("PWD")
                currentWorkingDirectory = parsePwdPath(pwdResp.message) ?: targetPath
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.DEBUG,
                    tag = "ClientBrowser",
                    message = "FTP CLIENT [CWD] Navigated to: '$currentWorkingDirectory'"
                )
                Result.success(currentWorkingDirectory)
            } else {
                Result.failure(IllegalStateException("Cannot change directory: ${resp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun navigateUp(): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))

        try {
            val resp = sendCommand("CDUP")
            if (resp.code in 200..299) {
                val pwdResp = sendCommand("PWD")
                currentWorkingDirectory = parsePwdPath(pwdResp.message) ?: "/"
                Result.success(currentWorkingDirectory)
            } else {
                // Fallback to CWD ..
                changeDirectory("..")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        localDestinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))

        val host = currentProfile?.host ?: "remote"
        val fileName = remotePath.substringAfterLast('/')

        try {
            // Check size if possible
            val sizeResp = sendCommand("SIZE $remotePath")
            val totalSize = if (sizeResp.code == 213) sizeResp.message.toLongOrNull() ?: 0L else 0L

            val dataSocket = openPassiveDataSocket()
                ?: return@withContext Result.failure(IllegalStateException("Failed to open data connection for download"))

            val retrResp = sendCommand("RETR $remotePath")
            if (retrResp.code !in 100..199) {
                try { dataSocket.close() } catch (ignored: Exception) {}
                return@withContext Result.failure(IllegalStateException("Download request rejected: ${retrResp.message}"))
            }

            localDestinationFile.parentFile?.mkdirs()
            val inputStream = dataSocket.getInputStream()
            val outputStream = FileOutputStream(localDestinationFile)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalDownloaded = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                onProgress(totalDownloaded, totalSize)
            }

            outputStream.flush()
            outputStream.close()
            try { dataSocket.close() } catch (ignored: Exception) {}

            val completeResp = readResponse()

            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.INFO,
                tag = "ClientTransfer",
                message = "FILE OPERATION [DOWNLOAD] Finished: '$fileName' ($totalDownloaded bytes) <- '$remotePath' from $host -> '${localDestinationFile.absolutePath}'"
            )

            Result.success(localDestinationFile)
        } catch (e: Exception) {
            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.ERROR,
                tag = "ClientTransfer",
                message = "FILE OPERATION [DOWNLOAD] Failed for '$remotePath': ${e.message}",
                throwable = e
            )
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        localFile: File,
        remoteFileName: String,
        onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))
        if (!localFile.exists() || !localFile.isFile) return@withContext Result.failure(IllegalArgumentException("Local file does not exist"))

        val host = currentProfile?.host ?: "remote"
        val totalSize = localFile.length()
        val remoteTargetPath = FtpListParser.buildFullPath(currentWorkingDirectory, remoteFileName)

        try {
            val dataSocket = openPassiveDataSocket()
                ?: return@withContext Result.failure(IllegalStateException("Failed to open data connection for upload"))

            val storResp = sendCommand("STOR $remoteFileName")
            if (storResp.code !in 100..199) {
                try { dataSocket.close() } catch (ignored: Exception) {}
                return@withContext Result.failure(IllegalStateException("Upload rejected: ${storResp.message}"))
            }

            val inputStream = FileInputStream(localFile)
            val outputStream = dataSocket.getOutputStream()

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalUploaded = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalUploaded += bytesRead
                onProgress(totalUploaded, totalSize)
            }

            outputStream.flush()
            inputStream.close()
            try { dataSocket.close() } catch (ignored: Exception) {}

            val completeResp = readResponse()

            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.INFO,
                tag = "ClientTransfer",
                message = "FILE OPERATION [UPLOAD] Finished: '${localFile.name}' ($totalUploaded bytes) -> '$remoteTargetPath' on $host"
            )

            Result.success("Upload complete: $remoteFileName")
        } catch (e: Exception) {
            AppLogHub.log(
                toolId = "ftp-client",
                toolName = "FTP Client",
                level = LogLevel.ERROR,
                tag = "ClientTransfer",
                message = "FILE OPERATION [UPLOAD] Failed for '${localFile.name}': ${e.message}",
                throwable = e
            )
            Result.failure(e)
        }
    }

    suspend fun deleteFile(remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))
        val host = currentProfile?.host ?: "remote"

        try {
            val resp = sendCommand("DELE $remotePath")
            if (resp.code in 200..299) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.INFO,
                    tag = "ClientFileOp",
                    message = "FILE OPERATION [DELETE] Deleted remote file: '$remotePath' on $host"
                )
                Result.success("Deleted $remotePath")
            } else {
                Result.failure(IllegalStateException("Failed to delete file: ${resp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(dirName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))
        val host = currentProfile?.host ?: "remote"

        try {
            val resp = sendCommand("MKD $dirName")
            if (resp.code in 200..299) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.INFO,
                    tag = "ClientFileOp",
                    message = "FILE OPERATION [MKDIR] Created remote directory: '$dirName' on $host"
                )
                Result.success("Created directory $dirName")
            } else {
                Result.failure(IllegalStateException("Failed to create directory: ${resp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeDirectory(dirPath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))
        val host = currentProfile?.host ?: "remote"

        try {
            val resp = sendCommand("RMD $dirPath")
            if (resp.code in 200..299) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.INFO,
                    tag = "ClientFileOp",
                    message = "FILE OPERATION [RMDDIR] Removed remote directory: '$dirPath' on $host"
                )
                Result.success("Removed directory $dirPath")
            } else {
                Result.failure(IllegalStateException("Failed to remove directory: ${resp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(fromPath: String, toPath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(IllegalStateException("Not connected to FTP server"))
        val host = currentProfile?.host ?: "remote"

        try {
            val rnfrResp = sendCommand("RNFR $fromPath")
            if (rnfrResp.code != 350) {
                return@withContext Result.failure(IllegalStateException("Cannot prepare rename for '$fromPath': ${rnfrResp.message}"))
            }

            val rntoResp = sendCommand("RNTO $toPath")
            if (rntoResp.code in 200..299) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.INFO,
                    tag = "ClientFileOp",
                    message = "FILE OPERATION [MOVE/RENAME] '$fromPath' -> '$toPath' on $host"
                )
                Result.success("Renamed '$fromPath' to '$toPath'")
            } else {
                Result.failure(IllegalStateException("Failed to rename: ${rntoResp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (isConnected) {
            val host = currentProfile?.host ?: ""
            try {
                sendCommand("QUIT")
            } catch (ignored: Exception) {}
            if (host.isNotBlank()) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.INFO,
                    tag = "ClientSession",
                    message = "FTP CLIENT [DISCONNECT] Session closed with $host"
                )
            }
        }
        try { reader?.close() } catch (ignored: Exception) {}
        try { writer?.close() } catch (ignored: Exception) {}
        try { controlSocket?.close() } catch (ignored: Exception) {}
        controlSocket = null
        reader = null
        writer = null
        currentProfile = null
    }

    private fun openPassiveDataSocket(): Socket? {
        val pasvResp = sendCommandSync("PASV")
        if (pasvResp.code != 227) return null

        val (ip, port) = parsePasvResponse(pasvResp.message) ?: return null
        val effectiveIp = if (ip == "127.0.0.1" || ip == "0.0.0.0") (currentProfile?.host ?: ip) else ip

        return try {
            val dataSocket = Socket()
            dataSocket.connect(InetSocketAddress(effectiveIp, port), 10000)
            dataSocket.soTimeout = 15000
            dataSocket
        } catch (e: Exception) {
            null
        }
    }

    private fun sendCommand(cmd: String): FtpResponse {
        val w = writer ?: throw IllegalStateException("Control writer is null")
        w.write("$cmd\r\n")
        w.flush()
        return readResponse()
    }

    private fun sendCommandSync(cmd: String): FtpResponse {
        return sendCommand(cmd)
    }

    private fun readResponse(): FtpResponse {
        val r = reader ?: throw IllegalStateException("Control reader is null")
        val sb = StringBuilder()
        var line = r.readLine() ?: throw IllegalStateException("Server closed control connection")
        sb.append(line)

        val code = line.take(3).toIntOrNull() ?: 0

        // Handle multiline replies: e.g. "220-Welcome..." until "220 Ready"
        if (line.length >= 4 && line[3] == '-') {
            val targetPrefix = "$code "
            while (true) {
                line = r.readLine() ?: break
                sb.append("\n").append(line)
                if (line.startsWith(targetPrefix)) {
                    break
                }
            }
        }

        val lastLine = sb.lines().lastOrNull { it.isNotBlank() } ?: ""
        val message = if (lastLine.length > 4) lastLine.substring(4).trim() else ""

        return FtpResponse(code = code, message = message, raw = sb.toString())
    }

    private fun parsePasvResponse(msg: String): Pair<String, Int>? {
        // e.g. "Entering Passive Mode (192,168,1,50,195,80)"
        val start = msg.indexOf('(')
        val end = msg.indexOf(')')
        if (start < 0 || end <= start) return null
        val parts = msg.substring(start + 1, end).split(',').map { it.trim().toIntOrNull() ?: return null }
        if (parts.size != 6) return null
        val ip = "${parts[0]}.${parts[1]}.${parts[2]}.${parts[3]}"
        val port = (parts[4] shl 8) + parts[5]
        return Pair(ip, port)
    }

    private fun parsePwdPath(msg: String): String? {
        val firstQuote = msg.indexOf('"')
        val secondQuote = msg.indexOf('"', firstQuote + 1)
        return if (firstQuote >= 0 && secondQuote > firstQuote) {
            msg.substring(firstQuote + 1, secondQuote)
        } else null
    }

    data class FtpResponse(
        val code: Int,
        val message: String,
        val raw: String,
    )
}
