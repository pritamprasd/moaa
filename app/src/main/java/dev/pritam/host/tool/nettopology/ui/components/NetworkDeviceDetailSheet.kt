package dev.pritam.host.tool.nettopology.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.tool.nettopology.model.*
import dev.pritam.host.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDeviceDetailSheet(
    node: NetworkNode,
    isPortScanning: Boolean,
    isTracerouting: Boolean,
    openPorts: List<PortInfo>,
    tracerouteHops: List<TracerouteHop>,
    onScanPorts: (String) -> Unit,
    onRunTraceroute: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = node.deviceType.defaultColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Color(0xFF334155)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. HEADER ROW ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(node.deviceType.emoji, fontSize = 22.sp)
                        }
                    }

                    Column {
                        Text(
                            text = node.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${node.deviceType.displayName} · ${node.ip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (node.isOnline) Color(0x2210B981) else Color(0x22EF4444),
                    border = BorderStroke(1.dp, if (node.isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                ) {
                    Text(
                        text = if (node.isOnline) "ONLINE" else "OFFLINE",
                        color = if (node.isOnline) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ── 2. QUICK ACTION BUTTONS ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { copyToClipboard(context, "IP Address", node.ip) },
                    shape = RoundedCornerShape(8.dp),
                    color = GlassSurfaceDeep,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋 Copy IP", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!node.mac.isNullOrBlank()) {
                    Surface(
                        onClick = { copyToClipboard(context, "MAC Address", node.mac) },
                        shape = RoundedCornerShape(8.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏷️ Copy MAC", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val hasWebPort = openPorts.any { it.port == 80 || it.port == 443 || it.port == 8080 }
                if (hasWebPort) {
                    val port = openPorts.first { it.port == 80 || it.port == 443 || it.port == 8080 }.port
                    val proto = if (port == 443) "https" else "http"
                    Surface(
                        onClick = {
                            val uri = Uri.parse("$proto://${node.ip}:$port")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x330284C7),
                        border = BorderStroke(1.dp, Cyan),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌐 Open Web", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── 3. DETAILED SPECIFICATIONS CARD ────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "NETWORK & HARDWARE PROPERTIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )

                    SpecRow("IP Address", node.ip, isMonospace = true)
                    SpecRow("MAC Address", node.mac ?: "Unavailable (Hidden / Randomized)", isMonospace = true)
                    SpecRow("Hardware Vendor", node.vendor ?: "Unknown Manufacturer")
                    SpecRow("Hostname / DNS", node.hostname ?: "No reverse DNS record")
                    SpecRow("Ping Latency", if (node.latencyMs != null) "${node.latencyMs} ms" else "Unknown")
                    SpecRow("Parent Node", node.parentIp ?: "Root Gateway / Direct")
                    SpecRow("Device Classification", node.deviceType.displayName)
                }
            }

            // ── 4. OPEN PORTS & SERVICE SCANNER ────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OPEN PORTS & SERVICES (${openPorts.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )

                        Surface(
                            onClick = { onScanPorts(node.ip) },
                            enabled = !isPortScanning,
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPortScanning) Color(0x33334155) else Color(0x3310B981),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isPortScanning) "Scanning..." else "⚡ Deep Scan",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (openPorts.isEmpty()) {
                        Text(
                            text = if (isPortScanning) "Probing common TCP ports (HTTP, SSH, FTP, RTSP, MQTT, DB)..." else "No open ports detected yet. Tap 'Deep Scan' to probe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            openPorts.forEach { port ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x221E293B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${port.port}/${port.protocol}",
                                            color = Cyan,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = port.serviceName,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0x2210B981)
                                    ) {
                                        Text(
                                            text = "OPEN",
                                            color = Color(0xFF34D399),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. TRACEROUTE & HOP DETECTION ──────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NETWORK ROUTE & TRACEROUTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )

                        Surface(
                            onClick = { onRunTraceroute(node.ip) },
                            enabled = !isTracerouting,
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTracerouting) Color(0x33334155) else Color(0x338B5CF6),
                            border = BorderStroke(1.dp, Violet.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isTracerouting) "Tracing..." else "🛰️ Trace Route",
                                color = Violet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (tracerouteHops.isEmpty()) {
                        Text(
                            text = if (isTracerouting) "Probing TTL hops to ${node.ip}..." else "Tap 'Trace Route' to inspect intermediate hops, serial routers, and switches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tracerouteHops.forEach { hop ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x221E293B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Hop #${hop.hopIndex}",
                                            color = Violet,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = hop.ip,
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (hop.rttMs != null) {
                                        Text(
                                            text = "${hop.rttMs} ms",
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. FULL SUMMARY COPY BUTTON ────────────────────────────
            Button(
                onClick = {
                    val fullSummary = buildString {
                        append("========================================\n")
                        append("  NETWORK DEVICE INSPECTION REPORT\n")
                        append("========================================\n")
                        append("Title: ${node.displayTitle}\n")
                        append("IP Address: ${node.ip}\n")
                        append("MAC Address: ${node.mac ?: "N/A"}\n")
                        append("Vendor: ${node.vendor ?: "Unknown"}\n")
                        append("Hostname: ${node.hostname ?: "N/A"}\n")
                        append("Device Type: ${node.deviceType.displayName}\n")
                        append("Latency: ${node.latencyMs ?: "N/A"} ms\n")
                        append("Parent Gateway: ${node.parentIp ?: "Root"}\n")
                        if (openPorts.isNotEmpty()) {
                            append("Open Ports:\n")
                            openPorts.forEach { append("  - ${it.port}/${it.protocol} (${it.serviceName})\n") }
                        }
                    }
                    copyToClipboard(context, "Device Report", fullSummary)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("📋 Copy Complete Node Report", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = 11.sp
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
