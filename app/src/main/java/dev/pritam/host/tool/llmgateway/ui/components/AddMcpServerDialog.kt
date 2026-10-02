package dev.pritam.host.tool.llmgateway.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurface
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType
import java.util.UUID

@Composable
fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (McpServerProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf(McpTransportType.HTTP_JSONRPC) }
    var endpointUrl by remember { mutableStateOf("http://192.168.1.") }
    var authToken by remember { mutableStateOf("") }
    var customHeaderKey by remember { mutableStateOf("") }
    var customHeaderVal by remember { mutableStateOf("") }

    val shape = RoundedCornerShape(16.dp)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = shape,
            color = SurfaceDeep,
            border = BorderStroke(1.dp, dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🔌 Connect Model Context Protocol (MCP) Server",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Connect external local or LAN MCP servers exposing JSON-RPC 2.0 tools (e.g. SQLite, Filesystem, GitHub, Weather).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(16.dp))

                // Transport Type Picker
                Text("TRANSPORT TYPE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(McpTransportType.HTTP_JSONRPC, McpTransportType.SSE).forEach { type ->
                        val isSel = transportType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Cyan.copy(alpha = 0.2f) else GlassSurface)
                                .border(BorderStroke(1.dp, if (isSel) Cyan else GlassBorder), RoundedCornerShape(8.dp))
                                .clickable {
                                    transportType = type
                                    if (type == McpTransportType.SSE && endpointUrl.endsWith("/mcp")) {
                                        endpointUrl = endpointUrl.removeSuffix("/mcp") + "/sse"
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${type.badge} ${type.displayName.take(16)}",
                                color = if (isSel) Cyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Server Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Friendly Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Home Desktop SQLite MCP", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Endpoint URL
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("MCP Endpoint URL", fontSize = 11.sp) },
                    placeholder = { Text("http://192.168.1.50:8000/mcp or http://localhost:3000/sse", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Optional Auth / Bearer Token
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("Authorization / Bearer Token (Optional)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. secret-token-xyz", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = {
                            val cleanName = if (name.isNotBlank()) name.trim() else "MCP Server (${endpointUrl.take(20)})"
                            val cleanEndpoint = endpointUrl.trim()
                            val headersMap = mutableMapOf<String, String>()
                            if (authToken.isNotBlank()) {
                                headersMap["Authorization"] = if (authToken.startsWith("Bearer ")) authToken.trim() else "Bearer ${authToken.trim()}"
                            }
                            if (customHeaderKey.isNotBlank() && customHeaderVal.isNotBlank()) {
                                headersMap[customHeaderKey.trim()] = customHeaderVal.trim()
                            }

                            val profile = McpServerProfile(
                                id = "mcp-${UUID.randomUUID()}",
                                name = cleanName,
                                transportType = transportType,
                                endpointUrl = cleanEndpoint,
                                headers = headersMap,
                                isEnabled = true
                            )
                            onAddServer(profile)
                            onDismiss()
                        },
                        enabled = endpointUrl.isNotBlank() && endpointUrl != "http://192.168.1.",
                        border = BorderStroke(1.dp, Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Cyan.copy(alpha = 0.15f),
                            contentColor = Cyan
                        )
                    ) {
                        Text("Connect & Sync Tools", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
