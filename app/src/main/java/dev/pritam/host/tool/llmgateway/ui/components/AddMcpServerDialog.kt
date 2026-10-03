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
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = shape,
            color = Color(0xFF12141C),
            border = BorderStroke(1.dp, Color(0xFF222531)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Cyan
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CONNECT MCP SERVER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Connect external local or LAN MCP servers exposing JSON-RPC 2.0 tools.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(16.dp))

                // Transport Type Picker
                Text("TRANSPORT TYPE", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(McpTransportType.HTTP_JSONRPC, McpTransportType.SSE).forEach { type ->
                        val isSel = transportType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Cyan.copy(alpha = 0.15f) else Color(0xFF0C0E14))
                                .border(BorderStroke(1.dp, if (isSel) Cyan else Color(0xFF222531)), RoundedCornerShape(8.dp))
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
                    placeholder = { Text("e.g. Home Desktop SQLite MCP", fontSize = 11.sp, color = Color(0xFF6B7280)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        focusedContainerColor = Color(0xFF0C0E14),
                        unfocusedContainerColor = Color(0xFF0C0E14),
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
                    placeholder = { Text("http://192.168.1.50:8000/mcp", fontSize = 11.sp, color = Color(0xFF6B7280)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        focusedContainerColor = Color(0xFF0C0E14),
                        unfocusedContainerColor = Color(0xFF0C0E14),
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
                    placeholder = { Text("e.g. secret-token-xyz", fontSize = 11.sp, color = Color(0xFF6B7280)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        focusedContainerColor = Color(0xFF0C0E14),
                        unfocusedContainerColor = Color(0xFF0C0E14),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    LiquidGlassButton(
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
                        modifier = Modifier.weight(1f),
                        glowColor = Cyan,
                        useRainbowBorder = false,
                        text = "Connect & Sync"
                    )
                }
            }
        }
    }
}
