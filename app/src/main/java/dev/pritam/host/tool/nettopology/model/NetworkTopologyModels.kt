package dev.pritam.host.tool.nettopology.model

import androidx.compose.ui.graphics.Color
import dev.pritam.host.ui.theme.*

enum class NetworkDeviceType(
    val displayName: String,
    val emoji: String,
    val defaultColor: Color
) {
    GATEWAY("Gateway / Router", "🌐", Cyan),
    SUB_ROUTER("Sub-Router / AP", "📡", Color(0xFF38BDF8)),
    LOCAL_HOST("This Device", "📱", Violet),
    MOBILE("Smartphone / Tablet", "📲", Color(0xFF60A5FA)),
    COMPUTER("Desktop / Laptop", "💻", Color(0xFF34D399)),
    SERVER_NAS("Server / NAS / Cloud", "🗄️", Color(0xFFFBBF24)),
    IOT_SMART("Smart Home / IoT", "💡", Color(0xFFA78BFA)),
    MEDIA_TV("Smart TV / Cast / Audio", "📺", Color(0xFFF472B6)),
    PRINTER("Network Printer", "🖨️", Color(0xFF94A3B8)),
    UNKNOWN("Network Node", "🔌", Color(0xFF64748B))
}

enum class GraphLayoutMode(val displayName: String, val icon: String) {
    HIERARCHICAL("Tree Hierarchy", "🌳"),
    RADIAL("Orbital Radar", "🎯"),
    MESH("Concentric Mesh", "🕸️")
}

data class PortInfo(
    val port: Int,
    val serviceName: String,
    val protocol: String = "TCP",
    val isOpen: Boolean = true,
    val banner: String? = null
)

data class TracerouteHop(
    val hopIndex: Int,
    val ip: String,
    val hostname: String? = null,
    val rttMs: Long? = null,
    val isReachable: Boolean = true
)

data class NetworkNode(
    val id: String, // IP Address or unique identifier
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val vendor: String? = null,
    val deviceType: NetworkDeviceType = NetworkDeviceType.UNKNOWN,
    val latencyMs: Long? = null,
    val parentIp: String? = null, // Used to build hierarchical tree / daisy-chain hops
    val hopCount: Int = 1,
    val isGateway: Boolean = false,
    val isLocalDevice: Boolean = false,
    val isOnline: Boolean = true,
    val openPorts: List<PortInfo> = emptyList(),
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isExpanded: Boolean = false // Collapsed by default as per project standard
) {
    val displayTitle: String
        get() = when {
            isLocalDevice -> "This Device (${hostname ?: ip})"
            isGateway -> "Default Gateway (${hostname ?: ip})"
            !hostname.isNullOrBlank() && hostname != ip -> hostname
            !vendor.isNullOrBlank() -> "$vendor ($ip)"
            else -> ip
        }

    val displaySubtitle: String
        get() = buildString {
            append(ip)
            if (!mac.isNullOrBlank()) append(" · $mac")
            if (latencyMs != null) append(" · ${latencyMs}ms")
        }
}

data class NetworkInterfaceInfo(
    val interfaceName: String = "wlan0",
    val localIp: String = "127.0.0.1",
    val subnetMask: String = "255.255.255.0",
    val gatewayIp: String = "192.168.1.1",
    val broadcastIp: String = "192.168.1.255",
    val dnsServers: List<String> = listOf("8.8.8.8", "1.1.1.1"),
    val ssid: String? = "Wi-Fi Network",
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val isConnected: Boolean = true
)

data class TopologyGraph(
    val rootGatewayIp: String,
    val nodes: List<NetworkNode>,
    val edges: List<TopologyEdge>,
    val scanTimestamp: Long = System.currentTimeMillis()
)

data class TopologyEdge(
    val fromIp: String,
    val toIp: String,
    val latencyMs: Long? = null,
    val isDirect: Boolean = true
)

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentIp: String = "",
    val scannedCount: Int = 0,
    val totalCount: Int = 254,
    val discoveredCount: Int = 0,
    val statusMessage: String = "Ready to scan"
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (scannedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}
