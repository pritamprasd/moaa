package dev.pritam.host.tool.nettopology.manager

import dev.pritam.host.tool.nettopology.model.PortInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

object PortScannerEngine {

    val COMMON_PORTS = listOf(
        PortInfo(21, "FTP (File Transfer)"),
        PortInfo(22, "SSH (Secure Shell)"),
        PortInfo(23, "Telnet"),
        PortInfo(53, "DNS Server"),
        PortInfo(80, "HTTP Web Server"),
        PortInfo(443, "HTTPS Secure Web"),
        PortInfo(554, "RTSP Camera Stream"),
        PortInfo(631, "IPP / CUPS Printing"),
        PortInfo(1883, "MQTT Broker (IoT)"),
        PortInfo(3306, "MySQL Database"),
        PortInfo(5000, "UPnP / Flask / Synology"),
        PortInfo(5353, "mDNS / Bonjour"),
        PortInfo(8000, "HTTP Dev / Web App"),
        PortInfo(8080, "HTTP Alternate / Proxy"),
        PortInfo(8443, "HTTPS Alternate"),
        PortInfo(9100, "Raw JetDirect Printer"),
        PortInfo(11434, "Ollama LLM API")
    )

    suspend fun scanPorts(ip: String, timeoutMs: Int = 300): List<PortInfo> = withContext(Dispatchers.IO) {
        coroutineScope {
            val scanJobs = COMMON_PORTS.map { portDef ->
                async {
                    checkPort(ip, portDef.port, portDef.serviceName, timeoutMs)
                }
            }
            scanJobs.awaitAll().filterNotNull()
        }
    }

    private fun checkPort(ip: String, port: Int, serviceName: String, timeoutMs: Int): PortInfo? {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            socket.soTimeout = timeoutMs

            var banner: String? = null
            if (port == 21 || port == 22 || port == 80 || port == 8080) {
                try {
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    if (reader.ready()) {
                        banner = reader.readLine()?.take(80)
                    }
                } catch (_: Exception) {
                    // Ignore banner read timeout
                }
            }

            PortInfo(
                port = port,
                serviceName = serviceName,
                protocol = "TCP",
                isOpen = true,
                banner = banner
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }
}
