package dev.pritam.host.ftp.server

import dev.pritam.host.ftp.model.FtpConfig
import dev.pritam.host.ftp.model.FtpLogEntry
import dev.pritam.host.ftp.model.FtpLogLevel
import dev.pritam.host.ftp.model.FtpServerState
import dev.pritam.host.ftp.model.FtpTelemetry
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe Coroutine-based FTP Server Engine.
 */
class FtpServerEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob: Job? = null
    private var speedMeterJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _serverState = MutableStateFlow<FtpServerState>(FtpServerState.Stopped)
    val serverState: StateFlow<FtpServerState> = _serverState.asStateFlow()

    private val _telemetry = MutableStateFlow(FtpTelemetry())
    val telemetry: StateFlow<FtpTelemetry> = _telemetry.asStateFlow()

    private val activeSessions = Collections.synchronizedList(mutableListOf<FtpSession>())
    private val maxRecentLogs = 100

    private val windowBytesUploaded = AtomicLong(0L)
    private val windowBytesDownloaded = AtomicLong(0L)

    @Synchronized
    fun start(config: FtpConfig, localIp: String) {
        if (_serverState.value is FtpServerState.Running || _serverState.value is FtpServerState.Starting) {
            return
        }

        _serverState.value = FtpServerState.Starting
        addLog(FtpLogEntry(level = FtpLogLevel.INFO, message = "Starting LAN FTP Server on $localIp:${config.port}..."))

        val rootDir = File(config.rootPath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        val fileSystem = FtpFileSystem(rootDir)

        serverJob = scope.launch {
            try {
                // Bind to 0.0.0.0 so clients on Wi-Fi/LAN can reach it
                val sSocket = ServerSocket(config.port, 50, InetAddress.getByName("0.0.0.0"))
                serverSocket = sSocket

                _serverState.value = FtpServerState.Running(
                    ipAddress = localIp,
                    port = config.port,
                    rootPath = config.rootPath,
                    username = config.username,
                )

                addLog(FtpLogEntry(level = FtpLogLevel.INFO, message = "Server listening on ftp://$localIp:${config.port}"))
                startSpeedMeter()

                while (isActive && !sSocket.isClosed) {
                    val clientSocket = try {
                        sSocket.accept()
                    } catch (e: Exception) {
                        if (!isActive || sSocket.isClosed) break
                        throw e
                    }

                    if (activeSessions.size >= config.maxClients) {
                        addLog(FtpLogEntry(
                            level = FtpLogLevel.WARNING,
                            message = "Rejecting connection from ${clientSocket.inetAddress.hostAddress}: Max clients limit reached (${config.maxClients})"
                        ))
                        try { clientSocket.close() } catch (ignored: Exception) {}
                        continue
                    }

                    val session = FtpSession(
                        controlSocket = clientSocket,
                        config = config,
                        fileSystem = fileSystem,
                        serverLocalIp = localIp,
                        onLog = { addLog(it) },
                        onBytesTransferred = { up, down ->
                            windowBytesUploaded.addAndGet(up)
                            windowBytesDownloaded.addAndGet(down)
                            _telemetry.update { current ->
                                current.copy(
                                    totalBytesUploaded = current.totalBytesUploaded + up,
                                    totalBytesDownloaded = current.totalBytesDownloaded + down,
                                )
                            }
                            updateClientTelemetry()
                        },
                        onSessionClosed = { closedSession ->
                            activeSessions.remove(closedSession)
                            updateClientTelemetry()
                        }
                    )

                    activeSessions.add(session)
                    updateClientTelemetry()

                    scope.launch {
                        session.run()
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    addLog(FtpLogEntry(level = FtpLogLevel.ERROR, message = "Server fatal error: ${e.message}"))
                    _serverState.value = FtpServerState.Error(message = e.localizedMessage ?: "Unknown server error", throwable = e)
                }
            } finally {
                stopInternal()
            }
        }
    }

    @Synchronized
    fun stop() {
        addLog(FtpLogEntry(level = FtpLogLevel.INFO, message = "Stopping FTP Server..."))
        stopInternal()
        _serverState.value = FtpServerState.Stopped
        addLog(FtpLogEntry(level = FtpLogLevel.INFO, message = "FTP Server stopped."))
    }

    fun disconnectClient(sessionId: String) {
        val session = synchronized(activeSessions) {
            activeSessions.firstOrNull { it.sessionId == sessionId }
        }
        session?.let {
            addLog(FtpLogEntry(level = FtpLogLevel.INFO, message = "Disconnecting client $sessionId by user request"))
            it.close()
        }
    }

    private fun stopInternal() {
        speedMeterJob?.cancel()
        speedMeterJob = null

        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null

        val currentSessions = ArrayList(activeSessions)
        for (session in currentSessions) {
            session.close()
        }
        activeSessions.clear()
        updateClientTelemetry()
    }

    private fun startSpeedMeter() {
        speedMeterJob?.cancel()
        speedMeterJob = scope.launch {
            while (isActive) {
                delay(1000)
                val up = windowBytesUploaded.getAndSet(0L)
                val down = windowBytesDownloaded.getAndSet(0L)
                _telemetry.update {
                    it.copy(
                        currentUploadSpeedBps = up,
                        currentDownloadSpeedBps = down,
                    )
                }
                updateClientTelemetry()
            }
        }
    }

    private fun updateClientTelemetry() {
        val clientInfos = synchronized(activeSessions) {
            activeSessions.map { it.getSessionInfo() }
        }
        _telemetry.update {
            it.copy(
                activeClientsCount = clientInfos.size,
                connectedClients = clientInfos,
            )
        }
    }

    private fun addLog(entry: FtpLogEntry) {
        _telemetry.update { current ->
            val updatedLogs = (listOf(entry) + current.recentLogs).take(maxRecentLogs)
            current.copy(recentLogs = updatedLogs)
        }

        val hubLevel = when (entry.level) {
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
            tag = "FtpEngine",
            message = entry.message
        )
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
