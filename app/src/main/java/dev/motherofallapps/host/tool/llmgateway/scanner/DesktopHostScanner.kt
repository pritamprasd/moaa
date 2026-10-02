package dev.motherofallapps.host.tool.llmgateway.scanner

import android.content.Context
import android.net.wifi.WifiManager
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmgateway.model.DiscoveredHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class DesktopHostScanner(private val context: Context) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    suspend fun scanLocalSubnet() = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        _scanProgress.value = 0f
        _discoveredHosts.value = emptyList()

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.INFO,
            tag = "Discovery",
            message = "LAN DISCOVERY [SCAN START] Starting LAN subnet sweep for desktop LLM hosts (Ollama, LM Studio, vLLM)..."
        )

        val subnetBase = getSubnetBase() ?: "192.168.1."
        val portsToCheck = listOf(
            11434 to "Ollama",
            1234 to "LM Studio",
            8000 to "vLLM / LocalAI"
        )

        val totalHosts = 254
        val foundHosts = mutableListOf<DiscoveredHost>()

        // Scan in parallel batches of 25 IPs for high speed without network congestion
        val batches = (1..totalHosts).chunked(25)
        var scannedCount = 0

        for (batch in batches) {
            val deferreds = batch.map { hostNum ->
                val ip = "$subnetBase$hostNum"
                async {
                    for ((port, serviceName) in portsToCheck) {
                        val isPortOpen = testSocketPort(ip, port, timeoutMs = 120)
                        if (isPortOpen) {
                            val probe = probeHost(ip, port, serviceName)
                            if (probe != null) {
                                synchronized(foundHosts) {
                                    foundHosts.add(probe)
                                    _discoveredHosts.value = foundHosts.toList()
                                }
                                AppLogHub.log(
                                    toolId = "llm-gateway",
                                    toolName = "LLM Gateway",
                                    level = LogLevel.INFO,
                                    tag = "Discovery",
                                    message = "LAN DISCOVERY [FOUND NODE] Discovered $serviceName host at http://$ip:$port (${probe.modelsAvailable.size} models)"
                                )
                            }
                        }
                    }
                }
            }

            deferreds.awaitAll()
            scannedCount += batch.size
            _scanProgress.value = scannedCount.toFloat() / totalHosts.toFloat()
        }

        _isScanning.value = false
        _scanProgress.value = 1f

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.INFO,
            tag = "Discovery",
            message = "LAN DISCOVERY [SCAN COMPLETE] Finished subnet sweep. Discovered ${foundHosts.size} desktop LLM endpoints."
        )
    }

    private fun testSocketPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun probeHost(ip: String, port: Int, guessedService: String): DiscoveredHost? {
        val start = System.currentTimeMillis()
        val urlBase = "http://$ip:$port"

        // Try /v1/models or /api/tags
        val endpoints = listOf("$urlBase/v1/models", "$urlBase/api/tags", urlBase)
        for (ep in endpoints) {
            try {
                val url = URL(ep)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 1500
                    readTimeout = 2000
                }

                val code = conn.responseCode
                val latency = System.currentTimeMillis() - start
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val models = parseModels(body)
                    val resolvedService = when {
                        body.contains("ollama", ignoreCase = true) || port == 11434 -> "Ollama"
                        port == 1234 -> "LM Studio"
                        else -> guessedService
                    }
                    return DiscoveredHost(
                        hostIp = ip,
                        port = port,
                        serviceType = resolvedService,
                        fullUrl = urlBase,
                        modelsAvailable = models,
                        responseTimeMs = latency
                    )
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
    }

    private fun parseModels(body: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val json = JSONObject(body)
            if (json.has("data")) {
                val data = json.getJSONArray("data")
                for (i in 0 until data.length()) {
                    val id = data.getJSONObject(i).optString("id", "")
                    if (id.isNotEmpty()) list.add(id)
                }
            } else if (json.has("models")) {
                val data = json.getJSONArray("models")
                for (i in 0 until data.length()) {
                    val name = data.getJSONObject(i).optString("name", "")
                    if (name.isNotEmpty()) list.add(name)
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return list
    }

    private fun getSubnetBase(): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: return null
            if (ipInt == 0) return null

            val ipString = String.format(
                "%d.%d.%d.",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff
            )
            return ipString
        } catch (e: Exception) {
            return null
        }
    }
}
