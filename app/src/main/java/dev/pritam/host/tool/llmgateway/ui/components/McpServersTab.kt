package dev.pritam.host.tool.llmgateway.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurface
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerStatus
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType

@Composable
fun McpServersTab(
    servers: List<McpServerProfile>,
    isTestingTool: Boolean,
    lastToolResult: McpToolResult?,
    onAddServerClick: () -> Unit,
    onToggleServerEnabled: (serverId: String, isEnabled: Boolean) -> Unit,
    onToggleToolEnabled: (serverId: String, toolName: String, isEnabled: Boolean) -> Unit,
    onSyncServerTools: (serverId: String) -> Unit,
    onPingServer: (serverId: String) -> Unit,
    onDeleteServer: (serverId: String) -> Unit,
    onTestRunTool: (toolName: String, argumentsJson: String) -> Unit,
    onClearTestResult: () -> Unit
) {
    var testingToolDef by remember { mutableStateOf<McpToolDefinition?>(null) }
    var expandedServerId by remember { mutableStateOf<String?>(null) }

    if (testingToolDef != null) {
        TestMcpToolDialog(
            tool = testingToolDef!!,
            isExecuting = isTestingTool,
            lastResult = lastToolResult,
            onExecute = { args -> onTestRunTool(testingToolDef!!.name, args) },
            onDismiss = {
                testingToolDef = null
                onClearTestResult()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top MCP Architecture Summary Banner
        item {
            McpSummaryCard(
                servers = servers,
                onAddServerClick = onAddServerClick
            )
        }

        // 2. Server List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTERED MCP SERVERS (${servers.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    fontSize = 11.sp
                )

                Text(
                    text = "${servers.flatMap { it.discoveredTools }.count { it.isEnabled }} active tools available",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        // 3. Server Profiles
        items(servers, key = { it.id }) { server ->
            McpServerCard(
                server = server,
                isExpanded = expandedServerId == server.id,
                onCardClick = {
                    expandedServerId = if (expandedServerId == server.id) null else server.id
                },
                onToggleEnabled = { onToggleServerEnabled(server.id, it) },
                onSyncTools = { onSyncServerTools(server.id) },
                onPing = { onPingServer(server.id) },
                onDelete = { onDeleteServer(server.id) },
                onToggleToolEnabled = { toolName, isEnabled -> onToggleToolEnabled(server.id, toolName, isEnabled) },
                onTestToolClick = { tool ->
                    testingToolDef = tool
                    onClearTestResult()
                }
            )
        }
    }
}

@Composable
private fun McpSummaryCard(
    servers: List<McpServerProfile>,
    onAddServerClick: () -> Unit
) {
    val totalTools = servers.flatMap { it.discoveredTools }.size
    val activeTools = servers.filter { it.isEnabled }.flatMap { it.discoveredTools }.count { it.isEnabled }
    val shape = RoundedCornerShape(14.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x351E293B), Color(0x220F172A))
                    )
                )
                .border(BorderStroke(1.dp, dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush), shape)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Model Context Protocol (MCP)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Connect external local or LAN MCP servers exposing JSON-RPC 2.0 tools. LLM Gateway auto-injects active tools into prompts and executes model tool calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = onAddServerClick,
                        border = BorderStroke(1.dp, Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Cyan.copy(alpha = 0.15f),
                            contentColor = Cyan
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("+ Add Server", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Stats Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge(label = "MCP SERVERS", value = "${servers.count { it.isEnabled }}/${servers.size}", color = Cyan, modifier = Modifier.weight(1f))
                    StatBadge(label = "ACTIVE TOOLS", value = "$activeTools ($totalTools total)", color = Emerald, modifier = Modifier.weight(1f))
                    StatBadge(label = "LOOPBACK API", value = "http://127.0.0.1:8080", color = Violet, modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurface)
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun McpServerCard(
    server: McpServerProfile,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSyncTools: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
    onToggleToolEnabled: (toolName: String, isEnabled: Boolean) -> Unit,
    onTestToolClick: (McpToolDefinition) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val isBuiltin = server.transportType == McpTransportType.BUILTIN_DEVICE

    val statusColor = when (server.status) {
        McpServerStatus.CONNECTED -> Emerald
        McpServerStatus.CONNECTING -> Cyan
        McpServerStatus.DISCONNECTED -> TextSecondary
        McpServerStatus.ERROR -> Rose
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isExpanded) Color(0x351E293B) else GlassSurfaceDeep,
                            Color(0x1D0F172A)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, if (server.isEnabled) dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush else BorderStroke(1.dp, GlassBorder).brush),
                    shape
                )
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Header + Transport + Status + Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCardClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(server.transportType.badge, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (server.isEnabled) TextPrimary else TextSecondary,
                                    fontSize = 13.sp
                                )
                                if (isBuiltin) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Emerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("BUILT-IN", color = Emerald, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${server.transportType.displayName} • ${server.endpointUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (server.latencyMs > 0) "${server.status.displayName} (${server.latencyMs}ms)" else server.status.displayName,
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = server.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Cyan,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = GlassSurface
                            )
                        )
                    }
                }

                // Error Banner if any
                if (server.errorMessage != null && server.status == McpServerStatus.ERROR) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Rose.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⚠️ ${server.errorMessage}",
                            color = Rose,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                // Action Bar
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onSyncTools,
                            border = BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🔄 Sync Tools (${server.discoveredTools.size})", fontSize = 10.sp)
                        }

                        if (!isBuiltin) {
                            OutlinedButton(
                                onClick = onPing,
                                border = BorderStroke(1.dp, GlassBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("⚡ Ping", fontSize = 10.sp)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isBuiltin) {
                            TextButton(
                                onClick = onDelete,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Delete", color = Rose, fontSize = 10.sp)
                            }
                        }

                        TextButton(
                            onClick = onCardClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Tools ▲" else "View Tools (${server.discoveredTools.size}) ▼",
                                color = Cyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Expandable Discovered Tools List
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )
                        Spacer(Modifier.height(10.dp))

                        if (server.discoveredTools.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurfaceDeep)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tools discovered yet. Tap 'Sync Tools' to retrieve tool schemas over JSON-RPC.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                server.discoveredTools.forEach { tool ->
                                    McpToolItemRow(
                                        tool = tool,
                                        onToggleEnabled = { onToggleToolEnabled(tool.name, it) },
                                        onTestClick = { onTestToolClick(tool) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun McpToolItemRow(
    tool: McpToolDefinition,
    onToggleEnabled: (Boolean) -> Unit,
    onTestClick: () -> Unit
) {
    var showSchema by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurface)
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙️",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tool.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (tool.isEnabled) Cyan else TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onTestClick,
                        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("▶ Test", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(6.dp))

                    Switch(
                        checked = tool.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = GlassSurfaceDeep
                        ),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            // Parameter schema chips
            if (tool.inputSchema.properties.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Params: ${tool.inputSchema.properties.keys.joinToString(", ")}",
                        fontFamily = FontFamily.Monospace,
                        color = Amber.copy(alpha = 0.8f),
                        fontSize = 9.sp
                    )

                    Text(
                        text = if (showSchema) "Hide Schema ▲" else "JSON Schema ▼",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable { showSchema = !showSchema }
                    )
                }

                if (showSchema) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassSurfaceDeep)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = tool.inputSchema.toJson().toString(2),
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun McpSummaryCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        McpSummaryCard(
            servers = listOf(
                dev.pritam.host.tool.llmgateway.mcp.model.McpServerProfile(
                    id = "sqlite",
                    name = "SQLite MCP Server",
                    transportType = dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType.SSE,
                    endpointUrl = "http://192.168.1.100:8000/sse",
                    isEnabled = true
                )
            ),
            onAddServerClick = {}
        )
    }
}


