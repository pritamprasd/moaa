package dev.pritam.host.tool.llmgateway.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.tool.llmgateway.model.DiscoveredHost
import dev.pritam.host.tool.llmgateway.model.LlmProfile
import dev.pritam.host.tool.llmgateway.model.ProfileStatus
import dev.pritam.host.tool.llmgateway.model.ProviderCategory
import dev.pritam.host.tool.llmgateway.model.RouterResult
import dev.pritam.host.tool.llmgateway.server.GatewayServerTelemetry
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmGatewayScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LlmGatewayViewModel = viewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val serverTelemetry by viewModel.serverTelemetry.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val discoveredHosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val pingingIds by viewModel.pingingProfileIds.collectAsStateWithLifecycle()
    val mcpServers by viewModel.mcpServers.collectAsStateWithLifecycle()
    val isTestingMcpTool by viewModel.isTestingMcpTool.collectAsStateWithLifecycle()
    val lastMcpToolResult by viewModel.lastMcpToolResult.collectAsStateWithLifecycle()

    var showAddCloudDialog by remember { mutableStateOf(false) }
    var showAddDesktopDialog by remember { mutableStateOf(false) }
    var showAddMcpServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LLM GATEWAY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    GlassBackButton(onClick = onNavigateBack)
                },
                actions = {
                    // Loopback server toggle pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (serverTelemetry.isRunning) Color(0xFF34D399).copy(alpha = 0.15f) else Rose.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose),
                        modifier = Modifier.clickable { viewModel.toggleServer() }
                    ) {
                        Text(
                            text = if (serverTelemetry.isRunning) "● 127.0.0.1:8080" else "○ STOPPED",
                            color = if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { viewModel.pingAllProfiles() }) {
                        Text("Ping All", color = Cyan, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tabs Bar
            val tabTitles = listOf("1. HOSTS", "2. ROUTING", "3. MCP", "4. TEST")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GlassSurfaceDeep,
                contentColor = Cyan,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Cyan,
                        height = 2.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(10.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Cyan else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            when (selectedTab) {
                0 -> AccountsAndHostsTab(
                    profiles = profiles,
                    isScanning = isScanning,
                    scanProgress = scanProgress,
                    discoveredHosts = discoveredHosts,
                    pingingIds = pingingIds,
                    onOpenAddCloud = { showAddCloudDialog = true },
                    onOpenAddDesktop = { showAddDesktopDialog = true },
                    onStartScan = { viewModel.startLanDiscovery() },
                    onAddDiscoveredHost = { viewModel.addDiscoveredHostAsProfile(it) },
                    onPingProfile = { viewModel.pingProfile(it) },
                    onToggleEnabled = { viewModel.toggleProfileEnabled(it) },
                    onDeleteProfile = { viewModel.deleteProfile(it.id) }
                )

                1 -> RoutingAndFailoverTab(
                    profiles = profiles,
                    onMovePriority = { from, to -> viewModel.movePriority(from, to) },
                    onToggleEnabled = { viewModel.toggleProfileEnabled(it) }
                )

                2 -> dev.pritam.host.tool.llmgateway.ui.components.McpServersTab(
                    servers = mcpServers,
                    isTestingTool = isTestingMcpTool,
                    lastToolResult = lastMcpToolResult,
                    onAddServerClick = { showAddMcpServerDialog = true },
                    onToggleServerEnabled = { serverId, isEnabled -> viewModel.toggleMcpServerEnabled(serverId, isEnabled) },
                    onToggleToolEnabled = { serverId, toolName, isEnabled -> viewModel.toggleMcpToolEnabled(serverId, toolName, isEnabled) },
                    onSyncServerTools = { serverId -> viewModel.syncMcpServerTools(serverId) },
                    onPingServer = { serverId -> viewModel.pingMcpServer(serverId) },
                    onDeleteServer = { serverId -> viewModel.deleteMcpServer(serverId) },
                    onTestRunTool = { toolName, argsJson -> viewModel.testRunMcpTool(toolName, argsJson) },
                    onClearTestResult = { viewModel.clearMcpTestResult() }
                )

                3 -> StatusAndLiveTestTab(
                    viewModel = viewModel,
                    serverTelemetry = serverTelemetry
                )
            }
        }
    }

    if (showAddCloudDialog) {
        AddCloudProfileDialog(
            onDismiss = { showAddCloudDialog = false },
            onSave = {
                viewModel.addCustomProfile(it)
                showAddCloudDialog = false
            }
        )
    }

    if (showAddDesktopDialog) {
        AddDesktopHostDialog(
            onDismiss = { showAddDesktopDialog = false },
            onSave = {
                viewModel.addCustomProfile(it)
                showAddDesktopDialog = false
            }
        )
    }

    if (showAddMcpServerDialog) {
        dev.pritam.host.tool.llmgateway.ui.components.AddMcpServerDialog(
            onDismiss = { showAddMcpServerDialog = false },
            onAddServer = {
                viewModel.addMcpServer(it)
                showAddMcpServerDialog = false
            }
        )
    }
}

@Composable
private fun AccountsAndHostsTab(
    profiles: List<LlmProfile>,
    isScanning: Boolean,
    scanProgress: Float,
    discoveredHosts: List<DiscoveredHost>,
    pingingIds: Set<String>,
    onOpenAddCloud: () -> Unit,
    onOpenAddDesktop: () -> Unit,
    onStartScan: () -> Unit,
    onAddDiscoveredHost: (DiscoveredHost) -> Unit,
    onPingProfile: (LlmProfile) -> Unit,
    onToggleEnabled: (LlmProfile) -> Unit,
    onDeleteProfile: (LlmProfile) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassButton(
                    onClick = onOpenAddDesktop,
                    modifier = Modifier.weight(1f),
                    glowColor = Color(0xFF34D399),
                    useRainbowBorder = true,
                    text = "+ Add Local Host"
                )

                LiquidGlassButton(
                    onClick = onOpenAddCloud,
                    modifier = Modifier.weight(1f),
                    glowColor = Cyan,
                    useRainbowBorder = true,
                    text = "+ Add Cloud Account"
                )
            }
        }

        // 2. Wi-Fi LAN Auto-Discovery Scanner Card
        item {
            IsometricCard(glowColor = Color(0xFF34D399)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WI-FI LAN AUTO-DISCOVERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF34D399),
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Sweep local subnet for Ollama (11434) & LM Studio (1234)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        LiquidGlassButton(
                            onClick = onStartScan,
                            enabled = !isScanning,
                            glowColor = Color(0xFF34D399),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            text = if (isScanning) "Scanning..." else "🔍 Scan LAN"
                        )
                    }

                    if (isScanning) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF34D399),
                            trackColor = GlassSurfaceDeep
                        )
                    }

                    if (discoveredHosts.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "DISCOVERED DESKTOP NODES (${discoveredHosts.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        discoveredHosts.forEach { host ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GlassSurfaceDeep,
                                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🖥️ ${host.serviceType} at ${host.hostIp}:${host.port}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Models: ${if (host.modelsAvailable.isNotEmpty()) host.modelsAvailable.joinToString(", ") else "Generic"} · ${host.responseTimeMs}ms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }

                                    LiquidGlassButton(
                                        onClick = { onAddDiscoveredHost(host) },
                                        glowColor = Color(0xFF34D399),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        text = "+ Add"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Configured Profiles Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED LLM PROFILES (${profiles.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // 4. Profiles Cards List
        items(profiles, key = { it.id }) { profile ->
            LlmProfileCard(
                profile = profile,
                isPinging = pingingIds.contains(profile.id),
                onPing = { onPingProfile(profile) },
                onToggleEnabled = { onToggleEnabled(profile) },
                onDelete = { onDeleteProfile(profile) }
            )
        }
    }
}

@Composable
private fun LlmProfileCard(
    profile: LlmProfile,
    isPinging: Boolean,
    onPing: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = Color(profile.status.badgeColorHex)
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Clean Liquid Shadow (No duplicate borders)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 3.dp)
                .clip(shape)
                .background(Color(0x45030712))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    BorderStroke(
                        1.dp,
                        if (profile.isEnabled) RainbowGlassBorderBrush else Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(alpha = 0.4f)))
                    ),
                    shape
                ),
            color = if (profile.isEnabled) GlassSurfaceElevated else GlassSurfaceDeep,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header: Category Icon + Profile Name + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (profile.category == ProviderCategory.DESKTOP_LOCAL_HOST) Color(0xFF34D399).copy(alpha = 0.2f) else Cyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${profile.category.icon} ${if (profile.category == ProviderCategory.DESKTOP_LOCAL_HOST) "DESKTOP" else "CLOUD"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (profile.category == ProviderCategory.DESKTOP_LOCAL_HOST) Color(0xFF34D399) else Cyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.isEnabled) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${profile.providerType} · Model: ${profile.targetModel ?: "default"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (profile.latencyMs > 0 && profile.status == ProfileStatus.ACTIVE) {
                                "${profile.status.displayName} (${profile.latencyMs}ms)"
                            } else {
                                profile.status.displayName
                            },
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Detail specs / endpoint
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GlassSurfaceDeep,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (profile.category == ProviderCategory.DESKTOP_LOCAL_HOST) {
                            Text("Endpoint: ${profile.hostAddress ?: "Not Set"}", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("Account: ${profile.accountEmail ?: "OAuth Session / API Key"}", color = TextSecondary, fontSize = 9.sp)
                        }
                        Text("Priority Rank: #${profile.priorityOrder + 1} in failover sequence", color = Cyan, fontSize = 9.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LiquidGlassButton(
                            onClick = onPing,
                            enabled = !isPinging,
                            glowColor = Cyan,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            text = if (isPinging) "Testing..." else "⚡ Ping Test"
                        )

                        LiquidGlassButton(
                            onClick = onToggleEnabled,
                            glowColor = if (profile.isEnabled) Color(0xFF34D399) else TextSecondary,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            text = if (profile.isEnabled) "✓ Enabled" else "○ Disabled"
                        )
                    }

                    LiquidGlassButton(
                        onClick = onDelete,
                        glowColor = Rose,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        text = "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutingAndFailoverTab(
    profiles: List<LlmProfile>,
    onMovePriority: (Int, Int) -> Unit,
    onToggleEnabled: (LlmProfile) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            IsometricCard(glowColor = Violet) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "INTELLIGENT ROUTING & FAILOVER RULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "The Gateway routes all incoming requests down this priority sequence. If a target is unreachable (Wi-Fi drop) or rate-limited (HTTP 429), it automatically fails over to the next enabled profile in zero downtime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "FAILOVER PRIORITY SEQUENCE (DRAG / REORDER):",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(profiles, key = { _, p -> p.id }) { index, profile ->
            val statusColor = Color(profile.status.badgeColorHex)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (profile.isEnabled) GlassSurfaceElevated else GlassSurfaceDeep,
                border = BorderStroke(1.dp, if (profile.isEnabled) RainbowGlassBorderBrush else Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(alpha = 0.4f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Rank Circle
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = if (index == 0) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                            border = BorderStroke(1.dp, if (index == 0) Cyan else GlassBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = if (index == 0) Cyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(profile.category.icon, fontSize = 10.sp)
                                Text(profile.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                if (index == 0) {
                                    Surface(shape = RoundedCornerShape(3.dp), color = Cyan.copy(alpha = 0.15f)) {
                                        Text("PRIMARY", color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(
                                text = "${profile.providerType} · ${profile.targetModel ?: "default"} · ${profile.status.displayName}",
                                color = statusColor,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Up / Down arrows
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LiquidGlassButton(
                            onClick = { onMovePriority(index, index - 1) },
                            enabled = index > 0,
                            glowColor = Cyan,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            text = "▲"
                        )

                        LiquidGlassButton(
                            onClick = { onMovePriority(index, index + 1) },
                            enabled = index < profiles.size - 1,
                            glowColor = Cyan,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            text = "▼"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusAndLiveTestTab(
    viewModel: LlmGatewayViewModel,
    serverTelemetry: GatewayServerTelemetry
) {
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Gateway Telemetry Card
        item {
            IsometricCard(glowColor = if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOCAL HTTP IPC SERVER STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (serverTelemetry.isRunning) "Endpoint: http://127.0.0.1:8080 (OpenAI Compatible)" else "Server is currently stopped.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        LiquidGlassButton(
                            onClick = { viewModel.toggleServer() },
                            glowColor = if (serverTelemetry.isRunning) Rose else Color(0xFF34D399),
                            useRainbowBorder = !serverTelemetry.isRunning,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            text = if (serverTelemetry.isRunning) "Stop Server" else "Start Server"
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("REQUESTS ROUTED", fontSize = 9.sp, color = TextSecondary)
                            Text("${serverTelemetry.totalRequestsRouted}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("FAILOVERS RECOVERED", fontSize = 9.sp, color = Violet)
                            Text("${serverTelemetry.failoversTriggered}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Violet)
                        }
                        Column {
                            Text("PORT", fontSize = 9.sp, color = Cyan)
                            Text("8080", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Cyan)
                        }
                    }
                }
            }
        }

        // 2. Live Sandbox Chat Title & Clear
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE INTERACTIVE TEST SANDBOX",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                if (chatMessages.isNotEmpty()) {
                    LiquidGlassButton(
                        onClick = { viewModel.clearTestChat() },
                        glowColor = Rose,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        text = "Clear Chat"
                    )
                }
            }
        }

        // 3. Chat Messages History
        items(chatMessages) { msg ->
            val isUser = msg.role == "user"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUser) Cyan.copy(alpha = 0.2f) else GlassSurfaceElevated,
                    border = BorderStroke(1.dp, if (isUser) Cyan.copy(alpha = 0.5f) else GlassBorderHighlight),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isUser) "YOU" else "GATEWAY ASSISTANT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) Cyan else Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = msg.content,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. Fallback trail info
        if (lastResult is RouterResult.FallbackSuccess) {
            val fallback = lastResult as RouterResult.FallbackSuccess
            item {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Violet.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Violet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ Auto-Failover Trail: ${fallback.attemptedProfiles.joinToString(" ➔ ")} ➔ [${fallback.finalProfile}]",
                        color = Violet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // 5. Input Text Field & Send Action
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { viewModel.setPromptInput(it) },
                    placeholder = { Text("Enter prompt for LLM Gateway...", fontSize = 11.sp) },
                    maxLines = 3,
                    colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preset query chip
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val presets = listOf("Quantum in 10 words", "Write Kotlin sum function", "Ollama status ping")
                        presets.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GlassSurfaceDeep,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier.clickable { viewModel.setPromptInput(p) }
                            ) {
                                Text(p, color = TextSecondary, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    LiquidGlassButton(
                        onClick = { viewModel.sendTestPrompt() },
                        enabled = !isGenerating && promptInput.isNotBlank(),
                        glowColor = Cyan,
                        useRainbowBorder = true,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        text = if (isGenerating) "Routing..." else "Send ➔"
                    )
                }
            }
        }
    }
}

