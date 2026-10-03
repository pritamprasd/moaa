package dev.pritam.host.ftp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ftp.model.FtpConfig
import dev.pritam.host.ftp.model.FtpServerState
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@Composable
fun ConnectionDetailsCard(
    serverState: FtpServerState,
    config: FtpConfig,
    localIp: String?,
    onCopyUrl: (String) -> Unit,
    onCopyCredentials: (String) -> Unit,
    onEditSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPassword by remember { mutableStateOf(false) }
    val isRunning = serverState is FtpServerState.Running
    val url = if (serverState is FtpServerState.Running) {
        serverState.connectionUrl
    } else {
        "ftp://${localIp ?: "192.168.x.x"}:${config.port}"
    }

    IsometricCard(
        modifier = modifier,
        glowColor = Cyan
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAN ACCESS CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )

                OutlinedButton(
                    onClick = onEditSettings,
                    enabled = !isRunning, // Cannot modify active port during execution
                    border = BorderStroke(1.dp, if (!isRunning) Cyan.copy(alpha = 0.5f) else Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (!isRunning) Cyan else TextSecondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Config", fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // FTP Server URL Box with 1-Tap Copy
            val urlShape = RoundedCornerShape(10.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(urlShape)
                    .clickable { onCopyUrl(url) },
                color = SurfaceDeep,
                shape = urlShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SERVER ADDRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Cyan else TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Cyan.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "COPY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Cyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Credentials row (Username & Password)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Username Box
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopyCredentials(config.username) },
                    color = SurfaceDeep,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "USERNAME",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = config.username,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Password Box
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showPassword = !showPassword },
                    color = SurfaceDeep,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASSWORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = if (showPassword) "HIDE" else "SHOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Violet,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (showPassword) config.password else "••••••••",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Violet,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun ConnectionDetailsCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        ConnectionDetailsCard(
            serverState = FtpServerState.Running(
                ipAddress = "192.168.1.105",
                port = 2121,
                rootPath = "/storage/emulated/0",
                username = "admin"
            ),
            config = FtpConfig(
                port = 2121,
                username = "admin",
                password = "secretpassword"
            ),
            localIp = "192.168.1.105",
            onCopyUrl = {},
            onCopyCredentials = {},
            onEditSettings = {}
        )
    }
}


