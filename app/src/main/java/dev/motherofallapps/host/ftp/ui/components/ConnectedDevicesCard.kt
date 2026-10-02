package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.motherofallapps.host.ftp.model.FtpClientSessionInfo
import dev.motherofallapps.host.ftp.storage.StorageUtils
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@Composable
fun ConnectedDevicesCard(
    clients: List<FtpClientSessionInfo>,
    isRunning: Boolean,
    onDisconnectClient: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    IsometricCard(
        modifier = modifier,
        glowColor = if (clients.isNotEmpty()) Cyan else Color(0xFF475569)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CONNECTED LAN DEVICES",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (clients.isNotEmpty()) Cyan else TextSecondary,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (clients.isNotEmpty()) Cyan.copy(alpha = 0.2f) else SurfaceDeep
                    ) {
                        Text(
                            text = "${clients.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (clients.isNotEmpty()) Cyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDeep)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start FTP server to allow LAN devices to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            } else if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDeep)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No devices connected right now · Waiting for LAN clients...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    clients.forEach { client ->
                        ClientDeviceItem(
                            client = client,
                            onDisconnect = { onDisconnectClient(client.sessionId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientDeviceItem(
    client: FtpClientSessionInfo,
    onDisconnect: () -> Unit,
) {
    val itemShape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .border(BorderStroke(1.dp, Color(0xFF334155)), itemShape),
        color = SurfaceDeep,
        shape = itemShape
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        text = "${client.ip}:${client.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDisconnect,
                    border = BorderStroke(1.dp, Rose.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Disconnect", fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "User: ${client.username ?: "Anonymous/Guest"} · Online: ${client.connectedDurationString}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Text(
                    text = "↓ ${StorageUtils.formatBytes(client.bytesDownloaded)}  ↑ ${StorageUtils.formatBytes(client.bytesUploaded)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Violet,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Status: ${client.currentActivity}",
                style = MaterialTheme.typography.bodySmall,
                color = Cyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
