package dev.pritam.host.tool.nettopology.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import dev.pritam.host.tool.nettopology.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class NetworkTopologyScanner(private val context: Context) {

    /**
     * Resolves the active network interface, local IP, gateway IP, and subnet properties.
     */
    fun resolveInterfaceInfo(): NetworkInterfaceInfo {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }

            var localIp = "127.0.0.1"
            var interfaceName = "wlan0"
            var gatewayIp = "192.168.1.1"
            val dnsServers = mutableListOf<String>()

            // Find non-loopback IPv4 address
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    val addrs = intf.inetAddresses.toList()
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            localIp = addr.hostAddress ?: localIp
                            interfaceName = intf.name
                            break
                        }
                    }
                }
            }

            // Extract Gateway & DNS from LinkProperties if available
            linkProps?.let { lp ->
                for (route in lp.routes) {
                    val gw = route.gateway
                    if (gw != null && gw is Inet4Address && !gw.isAnyLocalAddress) {
                        gatewayIp = gw.hostAddress ?: gatewayIp
                    }
                }
                for (dns in lp.dnsServers) {
                    if (dns is Inet4Address) {
                        dns.hostAddress?.let { dnsServers.add(it) }
                    }
                }
            }

            // Fallback gateway calculation if linkProps didn't provide one
            if (gatewayIp == "192.168.1.1" && localIp.contains(".")) {
                val lastDot = localIp.lastIndexOf('.')
                if (lastDot > 0) {
                    gatewayIp = "${localIp.substring(0, lastDot)}.1"
                }
            }

            // Extract Wi-Fi metadata if available
            var ssid: String? = null
            var linkSpeed: Int? = null
            var freq: Int? = null
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wifiManager?.connectionInfo
                if (info != null) {
                    ssid = info.ssid?.replace("\"", "")
                    if (ssid == "<unknown ssid>") ssid = "Local Wi-Fi Network"
                    linkSpeed = if (info.linkSpeed > 0) info.linkSpeed else null
                    freq = if (info.frequency > 0) info.frequency else null
                }
            } catch (_: Exception) {}

            return NetworkInterfaceInfo(
                interfaceName = interfaceName,
                localIp = localIp,
                subnetMask = "255.255.255.0",
                gatewayIp = gatewayIp,
                broadcastIp = getBroadcastAddress(localIp),
                dnsServers = if (dnsServers.isNotEmpty()) dnsServers else listOf("8.8.8.8", "1.1.1.1"),
                ssid = ssid ?: "Connected LAN / Wi-Fi",
                linkSpeedMbps = linkSpeed,
                frequencyMhz = freq,
                isConnected = localIp != "127.0.0.1"
            )
        } catch (e: Exception) {
            return NetworkInterfaceInfo()
        }
    }

    /**
     * Executes a high-speed parallel subnet scan, emitting live progress and discovered nodes.
     */
    fun scanSubnetFlow(
        interfaceInfo: NetworkInterfaceInfo,
        concurrency: Int = 32,
        probeTimeoutMs: Int = 350
    ): Flow<Pair<ScanProgress, List<NetworkNode>>> = flow {
        val baseIp = interfaceInfo.localIp
        val gatewayIp = interfaceInfo.gatewayIp
        val parts = baseIp.split(".")
        if (parts.size != 4) {
            emit(ScanProgress(statusMessage = "Invalid local IP address") to emptyList())
            return@flow
        }

        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
        val totalIps = 254
        val targetIps = (1..totalIps).map { "$prefix.$it" }

        val discoveredMap = ConcurrentHashMap<String, NetworkNode>()
        val scannedCounter = AtomicInteger(0)

        // Pre-parse ARP table
        val arpMap = ArpTableParser.readArpTable()

        // 1. Instantly register Gateway & Local Host
        val localMac = arpMap[baseIp]
        val localVendor = MacVendorResolver.resolveVendor(localMac)
        val localNode = NetworkNode(
            id = baseIp,
            ip = baseIp,
            mac = localMac,
            hostname = "localhost",
            vendor = localVendor ?: "Android Host Device",
            deviceType = NetworkDeviceType.LOCAL_HOST,
            latencyMs = 1L,
            parentIp = gatewayIp,
            isLocalDevice = true,
            isOnline = true
        )
        discoveredMap[baseIp] = localNode

        val gatewayMac = arpMap[gatewayIp]
        val gatewayVendor = MacVendorResolver.resolveVendor(gatewayMac)
        val gatewayNode = NetworkNode(
            id = gatewayIp,
            ip = gatewayIp,
            mac = gatewayMac,
            hostname = "Default Gateway",
            vendor = gatewayVendor ?: "Primary Router / Gateway",
            deviceType = NetworkDeviceType.GATEWAY,
            latencyMs = 2L,
            parentIp = null,
            isGateway = true,
            isOnline = true
        )
        discoveredMap[gatewayIp] = gatewayNode

        emit(
            ScanProgress(
                isScanning = true,
                scannedCount = 0,
                totalCount = totalIps,
                discoveredCount = discoveredMap.size,
                statusMessage = "Starting fast multi-protocol scan on $prefix.0/24..."
            ) to discoveredMap.values.toList()
        )

        // 2. Concurrently probe all IPs using Dispatchers.IO with chunked workers
        coroutineScope {
            val chunks = targetIps.chunked(concurrency)
            for (chunk in chunks) {
                chunk.map { ip ->
                    async(Dispatchers.IO) {
                        val node = probeIp(ip, interfaceInfo, arpMap, probeTimeoutMs)
                        val count = scannedCounter.incrementAndGet()
                        if (node != null) {
                            discoveredMap[ip] = node
                        }

                        if (count % 8 == 0 || count == totalIps) {
                            // Periodically re-check ARP table as new connections trigger kernel ARP updates
                            val freshArp = ArpTableParser.readArpTable()
                            freshArp.forEach { (arpIp, mac) ->
                                discoveredMap[arpIp]?.let { existing ->
                                    if (existing.mac.isNullOrBlank()) {
                                        val ven = MacVendorResolver.resolveVendor(mac)
                                        discoveredMap[arpIp] = existing.copy(
                                            mac = mac,
                                            vendor = existing.vendor ?: ven,
                                            deviceType = if (existing.deviceType == NetworkDeviceType.UNKNOWN) {
                                                MacVendorResolver.inferDeviceType(existing.isGateway, existing.isLocalDevice, existing.hostname, ven)
                                            } else existing.deviceType
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()

                val currentScanned = scannedCounter.get()
                emit(
                    ScanProgress(
                        isScanning = currentScanned < totalIps,
                        scannedCount = currentScanned,
                        totalCount = totalIps,
                        discoveredCount = discoveredMap.size,
                        statusMessage = "Scanning $prefix.* (${currentScanned}/$totalIps)"
                    ) to discoveredMap.values.toList()
                )
            }
        }

        // Final ARP resolution & topology linking pass
        val finalArp = ArpTableParser.readArpTable()
        val finalList = discoveredMap.values.map { node ->
            val mac = node.mac ?: finalArp[node.ip]
            val vendor = node.vendor ?: MacVendorResolver.resolveVendor(mac)
            val devType = if (node.deviceType == NetworkDeviceType.UNKNOWN) {
                MacVendorResolver.inferDeviceType(node.isGateway, node.isLocalDevice, node.hostname, vendor)
            } else node.deviceType

            node.copy(
                mac = mac,
                vendor = vendor,
                deviceType = devType
            )
        }.sortedWith(compareBy<NetworkNode> { !it.isGateway }
            .thenBy { !it.isLocalDevice }
            .thenBy { parseIpToLong(it.ip) })

        emit(
            ScanProgress(
                isScanning = false,
                scannedCount = totalIps,
                totalCount = totalIps,
                discoveredCount = finalList.size,
                statusMessage = "Scan completed. ${finalList.size} devices discovered."
            ) to finalList
        )
    }

    /**
     * Probes an individual IP address via ICMP ping and fast TCP socket fallback.
     */
    private fun probeIp(
        ip: String,
        interfaceInfo: NetworkInterfaceInfo,
        arpMap: Map<String, String>,
        timeoutMs: Int
    ): NetworkNode? {
        val isGateway = ip == interfaceInfo.gatewayIp
        val isLocal = ip == interfaceInfo.localIp

        if (isLocal) {
            val mac = arpMap[ip]
            val vendor = MacVendorResolver.resolveVendor(mac) ?: "Android Host Device"
            return NetworkNode(
                id = ip,
                ip = ip,
                mac = mac,
                hostname = "localhost",
                vendor = vendor,
                deviceType = NetworkDeviceType.LOCAL_HOST,
                latencyMs = 1L,
                parentIp = interfaceInfo.gatewayIp,
                isLocalDevice = true,
                isOnline = true
            )
        }

        var isReachable = false
        var latencyMs: Long? = null
        val startTime = System.currentTimeMillis()

        // 1. Try ICMP ping
        try {
            val addr = InetAddress.getByName(ip)
            if (addr.isReachable(timeoutMs)) {
                isReachable = true
                latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            }
        } catch (_: Exception) {}

        // 2. Fast TCP Socket probing fallback if ICMP failed
        val testPorts = intArrayOf(80, 443, 8080, 53, 22, 5000, 1883, 11434, 554, 9100)
        val openPorts = mutableListOf<PortInfo>()

        if (!isReachable) {
            for (port in testPorts) {
                var socket: Socket? = null
                try {
                    val socketStart = System.currentTimeMillis()
                    socket = Socket()
                    socket.connect(InetSocketAddress(ip, port), (timeoutMs / 2).coerceAtLeast(80))
                    isReachable = true
                    latencyMs = (System.currentTimeMillis() - socketStart).coerceAtLeast(1L)
                    openPorts.add(PortInfo(port = port, serviceName = "Port $port", isOpen = true))
                    break
                } catch (_: Exception) {
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
            }
        }

        // 3. If still unreachable, check if MAC exists in kernel ARP table
        val mac = arpMap[ip]
        if (!isReachable && mac != null && mac != "00:00:00:00:00:00") {
            isReachable = true
            latencyMs = 12L
        }

        if (!isReachable) return null

        // Resolve hostname
        var hostname: String? = null
        try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (canonical != ip && !canonical.isNullOrBlank()) {
                hostname = canonical
            } else {
                val host = addr.hostName
                if (host != ip && !host.isNullOrBlank()) {
                    hostname = host
                }
            }
        } catch (_: Exception) {}

        val vendor = MacVendorResolver.resolveVendor(mac)
        val devType = MacVendorResolver.inferDeviceType(isGateway, isLocal, hostname, vendor, openPorts.map { it.port })

        return NetworkNode(
            id = ip,
            ip = ip,
            mac = mac,
            hostname = hostname,
            vendor = vendor,
            deviceType = devType,
            latencyMs = latencyMs,
            parentIp = if (!isGateway) interfaceInfo.gatewayIp else null,
            isGateway = isGateway,
            isLocalDevice = isLocal,
            isOnline = true,
            openPorts = openPorts
        )
    }

    /**
     * Builds the complete topology graph with edges and hierarchical parents.
     */
    fun buildTopologyGraph(gatewayIp: String, nodes: List<NetworkNode>): TopologyGraph {
        val edges = mutableListOf<TopologyEdge>()

        // Identify any secondary routers / APs in the network
        val subRouters = nodes.filter { it.deviceType == NetworkDeviceType.SUB_ROUTER && it.ip != gatewayIp }

        for (node in nodes) {
            if (node.isGateway) continue

            // Determine parent: if there's a sub-router on the same IP cluster or daisy-chain, connect to it; else gateway
            val parentIp = if (subRouters.isNotEmpty() && !node.isLocalDevice && node.deviceType != NetworkDeviceType.SUB_ROUTER) {
                // Heuristic: check if matches sub-router IP range / cluster
                val sub = subRouters.firstOrNull { it.ip.substringBeforeLast('.') == node.ip.substringBeforeLast('.') }
                sub?.ip ?: gatewayIp
            } else {
                gatewayIp
            }

            edges.add(
                TopologyEdge(
                    fromIp = parentIp,
                    toIp = node.ip,
                    latencyMs = node.latencyMs,
                    isDirect = true
                )
            )
        }

        return TopologyGraph(
            rootGatewayIp = gatewayIp,
            nodes = nodes,
            edges = edges
        )
    }

    /**
     * Executes a real-time traceroute / hop probe to a target IP.
     */
    suspend fun runTraceroute(targetIp: String, maxHops: Int = 15): List<TracerouteHop> = withContext(Dispatchers.IO) {
        val hops = mutableListOf<TracerouteHop>()
        for (ttl in 1..maxHops) {
            val hopStart = System.currentTimeMillis()
            var hopIp: String? = null
            var reached = false

            try {
                // Simulate hop inspection via ping command or socket probe
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", "-t", ttl.toString(), targetIp))
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("From ") == true) {
                        val match = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b".toRegex().find(line ?: "")
                        hopIp = match?.value
                    } else if (line?.contains("bytes from") == true) {
                        hopIp = targetIp
                        reached = true
                    }
                }
                process.waitFor()
            } catch (_: Exception) {}

            val rtt = (System.currentTimeMillis() - hopStart).coerceAtLeast(1L)
            if (hopIp != null) {
                hops.add(
                    TracerouteHop(
                        hopIndex = ttl,
                        ip = hopIp,
                        rttMs = rtt,
                        isReachable = true
                    )
                )
                if (reached || hopIp == targetIp) break
            } else {
                hops.add(
                    TracerouteHop(
                        hopIndex = ttl,
                        ip = "*.*.*.* (Request timed out)",
                        rttMs = null,
                        isReachable = false
                    )
                )
            }
        }
        hops
    }

    private fun getBroadcastAddress(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.255" else "192.168.1.255"
    }

    private fun parseIpToLong(ip: String): Long {
        return try {
            val parts = ip.split(".")
            (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or (parts[2].toLong() shl 8) or parts[3].toLong()
        } catch (_: Exception) {
            0L
        }
    }
}
