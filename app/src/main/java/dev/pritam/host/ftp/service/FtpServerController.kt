package dev.pritam.host.ftp.service

import dev.pritam.host.ftp.model.FtpConfig
import dev.pritam.host.ftp.model.FtpServerState
import dev.pritam.host.ftp.model.FtpTelemetry
import dev.pritam.host.ftp.network.NetworkUtils
import dev.pritam.host.ftp.server.FtpServerEngine
import kotlinx.coroutines.flow.StateFlow

/**
 * Global singleton controller connecting UI, ViewModel, and the background Service.
 */
object FtpServerController {

    val engine = FtpServerEngine()

    val serverState: StateFlow<FtpServerState> = engine.serverState
    val telemetry: StateFlow<FtpTelemetry> = engine.telemetry

    var currentConfig: FtpConfig = FtpConfig()
        private set

    fun updateConfig(config: FtpConfig) {
        currentConfig = config
    }

    fun startServer(config: FtpConfig = currentConfig, ipOverride: String? = null) {
        currentConfig = config
        val ip = ipOverride ?: NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        engine.start(config, ip)
    }

    fun stopServer() {
        engine.stop()
    }
}
