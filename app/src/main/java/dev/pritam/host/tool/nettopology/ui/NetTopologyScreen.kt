package dev.pritam.host.tool.nettopology.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.host.tool.nettopology.model.*
import dev.pritam.host.tool.nettopology.ui.components.NetworkDeviceDetailSheet
import dev.pritam.host.tool.nettopology.ui.components.TopologyCanvasGraph
import dev.pritam.host.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetTopologyScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetTopologyViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interfaceInfo by viewModel.interfaceInfo.collectAsStateWithLifecycle()
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val isGraphView by viewModel.isGraphView.collectAsStateWithLifecycle()
    val expandedNodeIds by viewModel.expandedNodeIds.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()

    val isPortScanning by viewModel.isPortScanning.collectAsStateWithLifecycle()
    val nodeOpenPorts by viewModel.nodeOpenPorts.collectAsStateWithLifecycle()
    val isTracerouting by viewModel.isTracerouting.collectAsStateWithLifecycle()
    val nodeTraceroute by viewModel.nodeTraceroute.collectAsStateWithLifecycle()

    // Filtered nodes
    val filteredNodes = remember(nodes, searchQuery, selectedFilter) {
        nodes.filter { node ->
            val matchesFilter = selectedFilter == null || node.deviceType == selectedFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                node.ip.contains(searchQuery, ignoreCase = true) ||
                        (node.mac?.contains(searchQuery, ignoreCase = true) == true) ||
                        (node.hostname?.contains(searchQuery, ignoreCase = true) == true) ||
                        (node.vendor?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    // Modal Bottom Sheet for deep inspection
    selectedNode?.let { node ->
        NetworkDeviceDetailSheet(
            node = node,
            isPortScanning = isPortScanning,
            isTracerouting = isTracerouting,
            openPorts = nodeOpenPorts[node.ip] ?: node.openPorts,
            tracerouteHops = nodeTraceroute[node.ip] ?: emptyList(),
            onScanPorts = { ip -> viewModel.scanPortsForNode(ip) },
            onRunTraceroute = { ip -> viewModel.runTracerouteForNode(ip) },
            onDismiss = { viewModel.selectNode(null) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NETWORK TOPOLOGY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Cyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "SCANNER",
                                    color = Cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${interfaceInfo.ssid ?: "LAN"} · ${interfaceInfo.localIp} · ${nodes.size} Devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Cyan
                        )
                    }
                },
                actions = {
                    // Refresh / Rescan
                    IconButton(
                        onClick = { viewModel.startSubnetScan() },
                        enabled = !scanProgress.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Subnet",
                            tint = if (scanProgress.isScanning) TextSecondary else Cyan
                        )
                    }

                    // Copy Full Report
                    IconButton(onClick = { viewModel.copyNetworkReport(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Report",
                            tint = Violet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 1. KPI INTERFACE SUMMARY BAR ───────────────────────────
            NetworkKpiSummaryRow(
                interfaceInfo = interfaceInfo,
                totalNodes = nodes.size,
                isScanning = scanProgress.isScanning
            )

            // ── 2. SCAN PROGRESS INDICATOR ─────────────────────────────
            AnimatedVisibility(
                visible = scanProgress.isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x220284C7),
                    border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scanProgress.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(scanProgress.progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { scanProgress.progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Cyan,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // ── 3. VIEW MODE & LAYOUT CONTROLS ROW ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Graph View vs List View
                Row(
                    modifier = Modifier
                        .background(GlassSurfaceDeep, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ViewModeTabButton(
                        title = "🌐 Graph Map",
                        isSelected = isGraphView,
                        onClick = { viewModel.isGraphView.value = true }
                    )
                    ViewModeTabButton(
                        title = "📋 List View",
                        isSelected = !isGraphView,
                        onClick = { viewModel.isGraphView.value = false }
                    )
                }

                // Sub-Controls based on view mode
                if (isGraphView) {
                    // Layout Mode Switcher (Tree, Radial, Mesh)
                    Row(
                        modifier = Modifier
                            .background(GlassSurfaceDeep, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GraphLayoutMode.values().forEach { mode ->
                            LayoutModeIconButton(
                                icon = mode.icon,
                                tooltip = mode.displayName,
                                isSelected = layoutMode == mode,
                                onClick = { viewModel.layoutMode.value = mode }
                            )
                        }
                    }
                } else {
                    // Expand/Collapse All buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { viewModel.expandAllNodes() },
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "Expand All",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            onClick = { viewModel.collapseAllNodes() },
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "Collapse All",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── 4. SEARCH & DEVICE FILTER CHIPS ────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by IP, MAC, Hostname, or Vendor...", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Cyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurfaceDeep,
                    unfocusedContainerColor = GlassSurfaceDeep,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChipItem(
                        title = "All (${nodes.size})",
                        isSelected = selectedFilter == null,
                        color = Cyan,
                        onClick = { viewModel.selectedFilter.value = null }
                    )
                }
                items(NetworkDeviceType.values()) { type ->
                    val count = nodes.count { it.deviceType == type }
                    if (count > 0) {
                        FilterChipItem(
                            title = "${type.emoji} ${type.displayName} ($count)",
                            isSelected = selectedFilter == type,
                            color = type.defaultColor,
                            onClick = {
                                viewModel.selectedFilter.value = if (selectedFilter == type) null else type
                            }
                        )
                    }
                }
            }

            // ── 5. MAIN CONTENT (GRAPH VS LIST) ────────────────────────
            if (isGraphView) {
                TopologyCanvasGraph(
                    graph = graph,
                    layoutMode = layoutMode,
                    selectedNodeId = selectedNode?.id,
                    onSelectNode = { node -> viewModel.selectNode(node) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                if (filteredNodes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (scanProgress.isScanning) "Scanning local subnet for reachable devices..." else "No devices matching your search filter.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(filteredNodes, key = { it.id }) { node ->
                            val isExpanded = expandedNodeIds.contains(node.id)
                            val ports = nodeOpenPorts[node.ip] ?: node.openPorts

                            NetworkNodeListItem(
                                node = node,
                                isExpanded = isExpanded,
                                openPorts = ports,
                                onToggleExpand = { viewModel.toggleNodeExpansion(node.id) },
                                onInspectNode = { viewModel.selectNode(node) },
                                onCopy = { copyText ->
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Device Info", copyText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── SUB-COMPONENTS ─────────────────────────────────────────────────────────────

@Composable
private fun NetworkKpiSummaryRow(
    interfaceInfo: NetworkInterfaceInfo,
    totalNodes: Int,
    isScanning: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiMiniTile(
            label = "LOCAL IP",
            value = interfaceInfo.localIp,
            accentColor = Violet,
            modifier = Modifier.weight(1f)
        )
        KpiMiniTile(
            label = "GATEWAY",
            value = interfaceInfo.gatewayIp,
            accentColor = Cyan,
            modifier = Modifier.weight(1f)
        )
        KpiMiniTile(
            label = "DISCOVERED",
            value = "$totalNodes Nodes",
            accentColor = if (isScanning) Color(0xFFFBBF24) else Color(0xFF34D399),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiMiniTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = accentColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ViewModeTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Cyan.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = title,
            color = if (isSelected) Cyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun LayoutModeIconButton(
    icon: String,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Cyan.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)) else null
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp)
        ) {
            Text(icon, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.7f) else GlassBorder)
    ) {
        Text(
            text = title,
            color = if (isSelected) color else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NetworkNodeListItem(
    node: NetworkNode,
    isExpanded: Boolean,
    openPorts: List<PortInfo>,
    onToggleExpand: () -> Unit,
    onInspectNode: () -> Unit,
    onCopy: (String) -> Unit
) {
    val accentColor = node.deviceType.defaultColor

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (isExpanded) accentColor.copy(alpha = 0.4f) else GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Main clickable header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(node.deviceType.emoji, fontSize = 16.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.displayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            if (node.isLocalDevice) {
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Violet.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "HOST",
                                        color = Violet,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${node.ip} · ${node.vendor ?: node.deviceType.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Latency & Expand Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (node.latencyMs != null) {
                        Text(
                            text = "${node.latencyMs}ms",
                            color = if (node.latencyMs < 20) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Collapsed by default expandable drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = GlassBorder, thickness = 0.8.dp)

                    // Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MAC Address", color = TextSecondary, fontSize = 9.sp)
                            Text(node.mac ?: "Unavailable", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Parent / Hop", color = TextSecondary, fontSize = 9.sp)
                            Text(node.parentIp ?: "Gateway", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }

                    if (openPorts.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Open Ports: ", color = TextSecondary, fontSize = 9.sp)
                            Text(
                                text = openPorts.joinToString { "${it.port} (${it.serviceName})" },
                                color = Cyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Action buttons: Inspect Node, Copy IP, Copy MAC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onInspectNode,
                            shape = RoundedCornerShape(6.dp),
                            color = Cyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔍 Deep Inspect", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            onClick = { onCopy(node.ip) },
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceElevated,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📋 Copy IP", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        if (!node.mac.isNullOrBlank()) {
                            Surface(
                                onClick = { onCopy(node.mac) },
                                shape = RoundedCornerShape(6.dp),
                                color = GlassSurfaceElevated,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🏷️ Copy MAC", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NetTopologyScreenPreview() {
    Surface(color = SpaceBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Network Topology Screen Preview", color = Cyan)
        }
    }
}
