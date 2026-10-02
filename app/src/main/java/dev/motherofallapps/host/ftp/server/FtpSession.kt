package dev.motherofallapps.host.ftp.server

import dev.motherofallapps.host.ftp.model.FtpClientSessionInfo
import dev.motherofallapps.host.ftp.model.FtpConfig
import dev.motherofallapps.host.ftp.model.FtpLogEntry
import dev.motherofallapps.host.ftp.model.FtpLogLevel
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Handles the state and RFC 959/3659 commands for a single connected FTP client.
 */
class FtpSession(
    private val controlSocket: Socket,
    private val config: FtpConfig,
    private val fileSystem: FtpFileSystem,
    private val serverLocalIp: String,
    private val onLog: (FtpLogEntry) -> Unit,
    private val onBytesTransferred: (uploaded: Long, downloaded: Long) -> Unit,
    private val onSessionClosed: (FtpSession) -> Unit,
) {
    val clientIp: String = controlSocket.inetAddress.hostAddress ?: "unknown"
    val clientPort: Int = controlSocket.port
    val sessionId: String = "$clientIp:$clientPort"
    val connectedAtEpochMs: Long = System.currentTimeMillis()

    var authenticatedUser: String? = null
        private set
    var currentActivity: String = "Connected"
        private set
    var sessionBytesUploaded: Long = 0L
        private set
    var sessionBytesDownloaded: Long = 0L
        private set

    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private var isAuthenticated = false
    private var pendingUser: String? = null
    private var currentVirtualDir = "/"
    private var isBinaryMode = true
    private var renameFromTarget: File? = null
    private var restartOffset: Long = 0L

    // Passive & Active data connection state
    private var passiveServerSocket: ServerSocket? = null
    private var activeDataAddress: InetAddress? = null
    private var activeDataPort: Int = -1

    fun getSessionInfo(): FtpClientSessionInfo {
        return FtpClientSessionInfo(
            sessionId = sessionId,
            ip = clientIp,
            port = clientPort,
            connectedAtEpochMs = connectedAtEpochMs,
            username = authenticatedUser,
            currentActivity = currentActivity,
            bytesUploaded = sessionBytesUploaded,
            bytesDownloaded = sessionBytesDownloaded,
        )
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        try {
            controlSocket.soTimeout = 120_000 // 2 min idle timeout
            reader = BufferedReader(InputStreamReader(controlSocket.getInputStream(), Charsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(controlSocket.getOutputStream(), Charsets.UTF_8))

            log(FtpLogLevel.INFO, "Client connected: $sessionId")
            sendResponse(220, "MotherOfAllApps Android FTP Server ready")

            while (!controlSocket.isClosed) {
                val line = try {
                    reader?.readLine()
                } catch (e: SocketTimeoutException) {
                    log(FtpLogLevel.WARNING, "Connection timed out for $sessionId")
                    break
                } ?: break

                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val spaceIdx = trimmed.indexOf(' ')
                val command = (if (spaceIdx != -1) trimmed.substring(0, spaceIdx) else trimmed).uppercase()
                val argument = if (spaceIdx != -1) trimmed.substring(spaceIdx + 1).trim() else ""

                currentActivity = "Executing $command"
                val shouldContinue = handleCommand(command, argument)
                if (currentActivity.startsWith("Executing")) {
                    currentActivity = "Idle ($currentVirtualDir)"
                }

                if (!shouldContinue) {
                    break
                }
            }
        } catch (e: Exception) {
            log(FtpLogLevel.ERROR, "Session error for $sessionId: ${e.message}")
        } finally {
            close()
        }
    }

    private suspend fun handleCommand(cmd: String, arg: String): Boolean {
        // Unauthenticated commands permitted: USER, PASS, AUTH, FEAT, QUIT
        if (!isAuthenticated && cmd !in listOf("USER", "PASS", "AUTH", "FEAT", "QUIT", "NOOP")) {
            sendResponse(530, "Please login with USER and PASS.")
            return true
        }

        when (cmd) {
            "USER" -> handleUser(arg)
            "PASS" -> handlePass(arg)
            "AUTH" -> sendResponse(502, "Explicit TLS not implemented")
            "FEAT" -> handleFeat()
            "OPTS" -> handleOpts(arg)
            "SYST" -> sendResponse(215, "UNIX Type: L8")
            "PWD", "XPWD" -> sendResponse(257, "\"$currentVirtualDir\" is current directory")
            "TYPE" -> handleType(arg)
            "PASV" -> handlePasv()
            "EPSV" -> handleEpsv()
            "PORT" -> handlePort(arg)
            "EPRT" -> handleEprt(arg)
            "CWD", "XCWD" -> handleCwd(arg)
            "CDUP" -> handleCdup()
            "LIST" -> handleList(arg, isNameOnly = false)
            "NLST" -> handleList(arg, isNameOnly = true)
            "MLSD" -> handleMlsd(arg)
            "MLST" -> handleMlst(arg)
            "SIZE" -> handleSize(arg)
            "RETR" -> handleRetr(arg)
            "STOR" -> handleStor(arg, append = false)
            "APPE" -> handleStor(arg, append = true)
            "REST" -> handleRest(arg)
            "DELE" -> handleDele(arg)
            "MKD", "XMKD" -> handleMkd(arg)
            "RMD", "XRMD" -> handleRmd(arg)
            "RNFR" -> handleRnfr(arg)
            "RNTO" -> handleRnto(arg)
            "NOOP" -> sendResponse(200, "NOOP OK")
            "QUIT" -> {
                sendResponse(221, "Goodbye")
                return false
            }
            else -> sendResponse(502, "Command $cmd not implemented")
        }
        return true
    }

    private fun handleUser(user: String) {
        pendingUser = user
        if (config.isAnonymousAllowed && user.equals("anonymous", ignoreCase = true)) {
            sendResponse(331, "Guest login ok, send your complete e-mail address as password")
        } else {
            sendResponse(331, "User name okay, need password for $user")
        }
    }

    private fun handlePass(pass: String) {
        val user = pendingUser
        if (user == null) {
            sendResponse(503, "Login with USER first")
            return
        }

        val isValid = if (config.isAnonymousAllowed && user.equals("anonymous", ignoreCase = true)) {
            true
        } else {
            user == config.username && pass == config.password
        }

        if (isValid) {
            isAuthenticated = true
            authenticatedUser = user
            log(FtpLogLevel.AUTH, "Authenticated user '$user' from $sessionId")
            sendResponse(230, "User $user logged in")
        } else {
            isAuthenticated = false
            log(FtpLogLevel.AUTH, "Authentication FAILED for user '$user' from $sessionId")
            sendResponse(530, "User or password incorrect")
        }
    }

    private fun handleFeat() {
        writer?.let { w ->
            w.write("211-Features:\r\n")
            w.write(" UTF8\r\n")
            w.write(" SIZE\r\n")
            w.write(" MLST type*;size*;modify*;perm*;\r\n")
            w.write(" MLSD\r\n")
            w.write(" PASV\r\n")
            w.write(" EPSV\r\n")
            w.write(" REST STREAM\r\n")
            w.write("211 End\r\n")
            w.flush()
        }
    }

    private fun handleOpts(arg: String) {
        if (arg.uppercase().startsWith("UTF8")) {
            sendResponse(200, "UTF8 mode enabled")
        } else {
            sendResponse(501, "Option not supported")
        }
    }

    private fun handleType(arg: String) {
        when (arg.uppercase()) {
            "A", "A N" -> {
                isBinaryMode = false
                sendResponse(200, "Type set to ASCII")
            }
            "I", "L 8" -> {
                isBinaryMode = true
                sendResponse(200, "Type set to Image (Binary)")
            }
            else -> sendResponse(504, "Type $arg not supported")
        }
    }

    private fun handlePasv() {
        closeDataSockets()
        try {
            val s = ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))
            passiveServerSocket = s
            val port = s.localPort

            val ipParts = serverLocalIp.split(".").map { it.toInt() }
            val p1 = (port shr 8) and 0xFF
            val p2 = port and 0xFF

            val pasvStr = "${ipParts[0]},${ipParts[1]},${ipParts[2]},${ipParts[3]},$p1,$p2"
            sendResponse(227, "Entering Passive Mode ($pasvStr)")
        } catch (e: Exception) {
            sendResponse(425, "Cannot open passive data socket: ${e.message}")
        }
    }

    private fun handleEpsv() {
        closeDataSockets()
        try {
            val s = ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))
            passiveServerSocket = s
            val port = s.localPort
            sendResponse(229, "Entering Extended Passive Mode (|||$port|)")
        } catch (e: Exception) {
            sendResponse(425, "Cannot open extended passive data socket")
        }
    }

    private fun handlePort(arg: String) {
        closeDataSockets()
        try {
            val parts = arg.split(",").map { it.trim().toInt() }
            if (parts.size == 6) {
                val ip = "${parts[0]}.${parts[1]}.${parts[2]}.${parts[3]}"
                val port = (parts[4] shl 8) + parts[5]
                activeDataAddress = InetAddress.getByName(ip)
                activeDataPort = port
                sendResponse(200, "PORT command successful")
            } else {
                sendResponse(501, "Invalid PORT format")
            }
        } catch (e: Exception) {
            sendResponse(501, "PORT command error: ${e.message}")
        }
    }

    private fun handleEprt(arg: String) {
        closeDataSockets()
        try {
            val parts = arg.split("|")
            if (parts.size >= 4) {
                val ip = parts[2]
                val port = parts[3].toInt()
                activeDataAddress = InetAddress.getByName(ip)
                activeDataPort = port
                sendResponse(200, "EPRT command successful")
            } else {
                sendResponse(501, "Invalid EPRT format")
            }
        } catch (e: Exception) {
            sendResponse(501, "EPRT command error")
        }
    }

    private fun handleCwd(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && target.exists() && target.isDirectory) {
            currentVirtualDir = fileSystem.toVirtualPath(target)
            sendResponse(250, "Directory successfully changed to \"$currentVirtualDir\"")
        } else {
            sendResponse(550, "Failed to change directory: Not found or access denied")
        }
    }

    private fun handleCdup() {
        val parentVirtual = FtpFileSystem.normalizeVirtualPath("$currentVirtualDir/..")
        val target = fileSystem.resolve("/", parentVirtual)
        if (target != null && target.exists() && target.isDirectory) {
            currentVirtualDir = fileSystem.toVirtualPath(target)
            sendResponse(200, "Directory changed to \"$currentVirtualDir\"")
        } else {
            sendResponse(550, "Cannot change directory")
        }
    }

    private suspend fun handleList(arg: String, isNameOnly: Boolean) = withContext(Dispatchers.IO) {
        currentActivity = "Listing directory"
        val path = if (arg.startsWith("-")) "" else arg
        val target = fileSystem.resolve(currentVirtualDir, path)

        if (target == null || !target.exists()) {
            sendResponse(550, "File or directory not found")
            return@withContext
        }

        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Cannot open data connection")
            return@withContext
        }

        sendResponse(150, "Opening ASCII mode data connection for file list")

        try {
            val dWriter = BufferedWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8))
            val files = if (target.isDirectory) (target.listFiles() ?: emptyArray()) else arrayOf(target)

            for (f in files) {
                if (isNameOnly) {
                    dWriter.write("${f.name}\r\n")
                } else {
                    dWriter.write(fileSystem.formatUnixListing(f))
                }
            }
            dWriter.flush()
            sendResponse(226, "Transfer complete")
        } catch (e: Exception) {
            sendResponse(426, "Data transfer aborted: ${e.message}")
        } finally {
            try { dataSocket.close() } catch (ignored: Exception) {}
            closeDataSockets()
        }
    }

    private suspend fun handleMlsd(arg: String) = withContext(Dispatchers.IO) {
        currentActivity = "MLSD Listing"
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target == null || !target.exists() || !target.isDirectory) {
            sendResponse(550, "Directory not found")
            return@withContext
        }

        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Cannot open data connection")
            return@withContext
        }

        sendResponse(150, "Opening MLSD data connection")

        try {
            val dWriter = BufferedWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8))
            val files = target.listFiles() ?: emptyArray()

            for (f in files) {
                dWriter.write(fileSystem.formatMlsdEntry(f))
            }
            dWriter.flush()
            sendResponse(226, "MLSD transfer complete")
        } catch (e: Exception) {
            sendResponse(426, "MLSD aborted: ${e.message}")
        } finally {
            try { dataSocket.close() } catch (ignored: Exception) {}
            closeDataSockets()
        }
    }

    private fun handleMlst(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target == null || !target.exists()) {
            sendResponse(550, "File not found")
            return
        }

        writer?.let { w ->
            w.write("250- Listing ${target.name}\r\n")
            w.write(" " + fileSystem.formatMlsdEntry(target))
            w.write("250 End\r\n")
            w.flush()
        }
    }

    private fun handleSize(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && target.exists() && target.isFile) {
            sendResponse(213, target.length().toString())
        } else {
            sendResponse(550, "File not found or is a directory")
        }
    }

    private fun handleRest(arg: String) {
        try {
            restartOffset = arg.toLong().coerceAtLeast(0L)
            sendResponse(350, "Restarting at $restartOffset. Send STORE or RETRIEVE")
        } catch (e: Exception) {
            sendResponse(501, "Invalid REST parameter")
        }
    }

    private suspend fun handleRetr(arg: String) = withContext(Dispatchers.IO) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target == null || !target.exists() || !target.isFile) {
            sendResponse(550, "File not found or access denied")
            return@withContext
        }

        currentActivity = "Downloading ${target.name}"
        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Cannot open data connection")
            return@withContext
        }

        val offset = restartOffset
        restartOffset = 0L

        log(FtpLogLevel.TRANSFER, "Downloading ${target.name} (offset: $offset) for $sessionId")
        sendResponse(150, "Opening ${if (isBinaryMode) "BINARY" else "ASCII"} mode data connection for ${target.name}")

        try {
            val inStream = FileInputStream(target)
            if (offset > 0) {
                inStream.skip(offset)
            }
            val outStream = dataSocket.getOutputStream()
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalSent = 0L

            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                outStream.write(buffer, 0, bytesRead)
                totalSent += bytesRead
                sessionBytesDownloaded += bytesRead
                onBytesTransferred(0L, bytesRead.toLong())
            }
            outStream.flush()
            inStream.close()

            log(FtpLogLevel.TRANSFER, "FILE OPERATION [DOWNLOAD] Finished: '${target.name}' ($totalSent bytes) <- '${fileSystem.toVirtualPath(target)}' for $sessionId")
            sendResponse(226, "Transfer complete")
        } catch (e: Exception) {
            log(FtpLogLevel.ERROR, "FILE OPERATION [DOWNLOAD] Failed for '${target.name}': ${e.message}")
            sendResponse(426, "Data connection error: ${e.message}")
        } finally {
            try { dataSocket.close() } catch (ignored: Exception) {}
            closeDataSockets()
        }
    }

    private suspend fun handleStor(arg: String, append: Boolean) = withContext(Dispatchers.IO) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target == null) {
            sendResponse(553, "File name not allowed or path security violation")
            return@withContext
        }

        currentActivity = "Uploading ${target.name}"
        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Cannot open data connection")
            return@withContext
        }

        val offset = restartOffset
        restartOffset = 0L

        log(FtpLogLevel.TRANSFER, "FILE OPERATION [UPLOAD] Started: '${target.name}' (append: $append) from $sessionId")
        sendResponse(150, "Opening data connection for file upload")

        try {
            val outStream = FileOutputStream(target, append || offset > 0)
            val inStream = dataSocket.getInputStream()
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalReceived = 0L

            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                outStream.write(buffer, 0, bytesRead)
                totalReceived += bytesRead
                sessionBytesUploaded += bytesRead
                onBytesTransferred(bytesRead.toLong(), 0L)
            }
            outStream.flush()
            outStream.close()

            log(FtpLogLevel.TRANSFER, "FILE OPERATION [UPLOAD] Finished: '${target.name}' ($totalReceived bytes) -> '${fileSystem.toVirtualPath(target)}' from $sessionId")
            sendResponse(226, "Transfer complete")
        } catch (e: Exception) {
            log(FtpLogLevel.ERROR, "FILE OPERATION [UPLOAD] Failed for '${target.name}': ${e.message}")
            sendResponse(426, "Data transfer failed: ${e.message}")
        } finally {
            try { dataSocket.close() } catch (ignored: Exception) {}
            closeDataSockets()
        }
    }

    private fun handleDele(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && target.exists() && target.isFile) {
            if (target.delete()) {
                log(FtpLogLevel.INFO, "FILE OPERATION [DELETE] Deleted file: '${target.name}' at '${fileSystem.toVirtualPath(target)}' by $sessionId")
                sendResponse(250, "File deleted successfully")
            } else {
                sendResponse(550, "Failed to delete file")
            }
        } else {
            sendResponse(550, "File not found or is a directory")
        }
    }

    private fun handleMkd(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && !target.exists()) {
            if (target.mkdirs()) {
                log(FtpLogLevel.INFO, "FILE OPERATION [MKDIR] Created directory: '${target.name}' at '${fileSystem.toVirtualPath(target)}' by $sessionId")
                sendResponse(257, "\"${fileSystem.toVirtualPath(target)}\" created")
            } else {
                sendResponse(550, "Failed to create directory")
            }
        } else {
            sendResponse(550, "Directory already exists or invalid path")
        }
    }

    private fun handleRmd(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && target.exists() && target.isDirectory) {
            if (target.delete()) {
                log(FtpLogLevel.INFO, "FILE OPERATION [RMDDIR] Removed directory: '${target.name}' by $sessionId")
                sendResponse(250, "Directory removed")
            } else {
                sendResponse(550, "Directory not empty or cannot be removed")
            }
        } else {
            sendResponse(550, "Directory not found")
        }
    }

    private fun handleRnfr(arg: String) {
        val target = fileSystem.resolve(currentVirtualDir, arg)
        if (target != null && target.exists()) {
            renameFromTarget = target
            sendResponse(350, "File exists, ready for destination name (RNTO)")
        } else {
            sendResponse(550, "File not found")
        }
    }

    private fun handleRnto(arg: String) {
        val source = renameFromTarget
        if (source == null) {
            sendResponse(503, "Bad sequence of commands: RNFR first")
            return
        }
        renameFromTarget = null

        val dest = fileSystem.resolve(currentVirtualDir, arg)
        if (dest != null && !dest.exists()) {
            if (source.renameTo(dest)) {
                log(FtpLogLevel.INFO, "FILE OPERATION [MOVE/RENAME] '${source.name}' -> '${dest.name}' at '${fileSystem.toVirtualPath(dest)}' by $sessionId")
                sendResponse(250, "File renamed successfully")
            } else {
                sendResponse(550, "Rename operation failed")
            }
        } else {
            sendResponse(553, "Destination exists or invalid path")
        }
    }

    private fun openDataSocket(): Socket? {
        val pss = passiveServerSocket
        if (pss != null) {
            return try {
                pss.soTimeout = 15_000
                pss.accept()
            } catch (e: Exception) {
                null
            }
        }

        val addr = activeDataAddress
        val port = activeDataPort
        if (addr != null && port > 0) {
            return try {
                Socket(addr, port)
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    private fun closeDataSockets() {
        try {
            passiveServerSocket?.close()
        } catch (ignored: Exception) {}
        passiveServerSocket = null
        activeDataAddress = null
        activeDataPort = -1
    }

    private fun sendResponse(code: Int, message: String) {
        try {
            writer?.let {
                it.write("$code $message\r\n")
                it.flush()
            }
        } catch (ignored: Exception) {}
    }

    private fun log(level: FtpLogLevel, message: String) {
        val entry = FtpLogEntry(level = level, message = message, clientIp = clientIp)
        onLog(entry)

        // Push to Central Diagnostic Log Hub
        val hubLevel = when (level) {
            FtpLogLevel.INFO -> LogLevel.INFO
            FtpLogLevel.TRANSFER -> LogLevel.DEBUG
            FtpLogLevel.AUTH -> LogLevel.INFO
            FtpLogLevel.WARNING -> LogLevel.WARN
            FtpLogLevel.ERROR -> LogLevel.ERROR
        }
        AppLogHub.log(
            toolId = "ftp-server",
            toolName = "LAN FTP Server",
            level = hubLevel,
            tag = "FtpSession",
            message = "[$sessionId] $message"
        )
    }

    fun close() {
        closeDataSockets()
        try {
            reader?.close()
        } catch (ignored: Exception) {}
        try {
            writer?.close()
        } catch (ignored: Exception) {}
        try {
            controlSocket.close()
        } catch (ignored: Exception) {}
        onSessionClosed(this)
    }
}
