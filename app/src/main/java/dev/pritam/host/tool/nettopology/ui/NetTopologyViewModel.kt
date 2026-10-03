package dev.pritam.host.tool.nettopology.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.host.tool.nettopology.manager.NetworkTopologyScanner
import dev.pritam.host.tool.nettopology.manager.PortScannerEngine
import dev.pritam.host.tool.nettopology.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetTopologyViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = NetworkTopologyScanner(application)

    private val _interfaceInfo = MutableStateFlow(NetworkInterfaceInfo())
    val interfaceInfo: StateFlow<NetworkInterfaceInfo> = _interfaceInfo.asStateFlow()

    private val _nodes = MutableStateFlow<List<NetworkNode>>(emptyList())
    val nodes: StateFlow<List<NetworkNode>> = _nodes.asStateFlow()

    private val _graph = MutableStateFlow(TopologyGraph("192.168.1.1", emptyList(), emptyList()))
    val graph: StateFlow<TopologyGraph> = _graph.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow<NetworkDeviceType?>(null)
    val layoutMode = MutableStateFlow(GraphLayoutMode.HIERARCHICAL)
    val isGraphView = MutableStateFlow(true) // Default to Visual Graph view

    // Collapsed containers by default (empty set of expanded item IDs)
    private val _expandedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedNodeIds: StateFlow<Set<String>> = _expandedNodeIds.asStateFlow()

    private val _selectedNode = MutableStateFlow<NetworkNode?>(null)
    val selectedNode: StateFlow<NetworkNode?> = _selectedNode.asStateFlow()

    private val _isPortScanning = MutableStateFlow(false)
    val isPortScanning: StateFlow<Boolean> = _isPortScanning.asStateFlow()

    private val _nodeOpenPorts = MutableStateFlow<Map<String, List<PortInfo>>>(emptyMap())
    val nodeOpenPorts: StateFlow<Map<String, List<PortInfo>>> = _nodeOpenPorts.asStateFlow()

    private val _isTracerouting = MutableStateFlow(false)
    val isTracerouting: StateFlow<Boolean> = _isTracerouting.asStateFlow()

    private val _nodeTraceroute = MutableStateFlow<Map<String, List<TracerouteHop>>>(emptyMap())
    val nodeTraceroute: StateFlow<Map<String, List<TracerouteHop>>> = _nodeTraceroute.asStateFlow()

    private var scanJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        refreshInterface()
        startSubnetScan()
    }

    fun refreshInterface() {
        _interfaceInfo.value = scanner.resolveInterfaceInfo()
    }

    fun startSubnetScan() {
        scanJob?.cancel()
        refreshInterface()

        val info = _interfaceInfo.value
        scanJob = viewModelScope.launch {
            scanner.scanSubnetFlow(info).collect { (progress, nodeList) ->
                _scanProgress.value = progress
                _nodes.value = nodeList
                _graph.value = scanner.buildTopologyGraph(info.gatewayIp, nodeList)
            }
        }
    }

    fun selectNode(node: NetworkNode?) {
        _selectedNode.value = node
        if (node != null && !_nodeOpenPorts.value.containsKey(node.ip)) {
            // Auto-trigger port scan for selected node if not yet cached
            scanPortsForNode(node.ip)
        }
    }

    fun toggleNodeExpansion(nodeId: String) {
        val current = _expandedNodeIds.value
        _expandedNodeIds.value = if (current.contains(nodeId)) {
            current - nodeId
        } else {
            current + nodeId
        }
    }

    fun expandAllNodes() {
        _expandedNodeIds.value = _nodes.value.map { it.id }.toSet()
    }

    fun collapseAllNodes() {
        _expandedNodeIds.value = emptySet()
    }

    fun scanPortsForNode(ip: String) {
        viewModelScope.launch {
            _isPortScanning.value = true
            val ports = PortScannerEngine.scanPorts(ip)
            _nodeOpenPorts.value = _nodeOpenPorts.value + (ip to ports)

            // Update open ports in node list
            _nodes.value = _nodes.value.map {
                if (it.ip == ip) it.copy(openPorts = ports) else it
            }
            _graph.value = scanner.buildTopologyGraph(_interfaceInfo.value.gatewayIp, _nodes.value)
            _isPortScanning.value = false
        }
    }

    fun runTracerouteForNode(ip: String) {
        viewModelScope.launch {
            _isTracerouting.value = true
            val hops = scanner.runTraceroute(ip)
            _nodeTraceroute.value = _nodeTraceroute.value + (ip to hops)
            _isTracerouting.value = false
        }
    }

    fun copyNetworkReport(context: Context) {
        val nodesList = _nodes.value
        val info = _interfaceInfo.value
        val report = buildString {
            append("============================================================\n")
            append("  MOTHER OF ALL APPS · NETWORK TOPOLOGY REPORT\n")
            append("============================================================\n")
            append("Network: ${info.ssid} (${info.interfaceName})\n")
            append("Local IP: ${info.localIp} · Gateway: ${info.gatewayIp}\n")
            append("Discovered Devices: ${nodesList.size}\n\n")

            nodesList.forEachIndexed { i, node ->
                append("------------------------------------------------------------\n")
                append("#${i + 1} ${node.displayTitle}\n")
                append("  Type: ${node.deviceType.displayName}\n")
                append("  IP: ${node.ip}\n")
                append("  MAC: ${node.mac ?: "N/A"}\n")
                append("  Vendor: ${node.vendor ?: "Unknown"}\n")
                append("  Latency: ${node.latencyMs ?: "N/A"} ms\n")
                val ports = _nodeOpenPorts.value[node.ip] ?: node.openPorts
                if (ports.isNotEmpty()) {
                    append("  Open Ports: ${ports.joinToString { "${it.port}/${it.serviceName}" }}\n")
                }
            }
            append("============================================================\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Network Topology Report", report))
        Toast.makeText(context, "Copied full network report (${nodesList.size} devices)", Toast.LENGTH_SHORT).show()
    }
}
