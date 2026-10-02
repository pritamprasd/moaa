package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.motherofallapps.host.ftp.model.FtpServerState
import dev.motherofallapps.host.ftp.network.NetworkUtils
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@Composable
fun IsometricControlHeader(
    serverState: FtpServerState,
    networkStatus: NetworkUtils.NetworkStatus,
    onToggleServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = serverState is FtpServerState.Running
    val isStarting = serverState is FtpServerState.Starting

    val statusColor by animateColorAsState(
        targetValue = if (isRunning) Cyan else if (isStarting) Violet else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "status_color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status and Network Badge
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = statusColor
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "SERVER ONLINE" else if (isStarting) "STARTING..." else "SERVER OFFLINE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${networkStatus.networkName ?: "LAN"} · ${networkStatus.localIpAddress ?: "No Wi-Fi"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // 3D Isometric Power Toggle Button
            IsometricPowerButton(
                isRunning = isRunning,
                isStarting = isStarting,
                onClick = onToggleServer
            )
        }
    }
}

@Composable
private fun IsometricPowerButton(
    isRunning: Boolean,
    isStarting: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonColor = if (isRunning) Rose else Cyan
    val textColor = Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .clickable(enabled = !isStarting) { onClick() }
    ) {
        // Isometric 3D Base Drop
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(shape)
                .background(Color(0xFF070A10))
        )

        // Button Surface
        Surface(
            modifier = Modifier
                .clip(shape)
                .border(
                    BorderStroke(1.dp, buttonColor.copy(alpha = 0.8f)),
                    shape
                ),
            shape = shape,
            color = buttonColor
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "STOP SERVER" else if (isStarting) "LAUNCHING..." else "START SERVER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
