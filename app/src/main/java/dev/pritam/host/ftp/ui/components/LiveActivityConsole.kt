package dev.pritam.host.ftp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ftp.model.FtpLogEntry
import dev.pritam.host.ftp.model.FtpLogLevel
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@Composable
fun LiveActivityConsole(
    logs: List<FtpLogEntry>,
    modifier: Modifier = Modifier,
) {
    IsometricCard(
        modifier = modifier,
        glowColor = Violet
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE ACTIVITY CONSOLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Violet,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
                Text(
                    text = "${logs.size} events",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDeep)
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Server idle · Waiting for LAN client connections...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(logs, key = { "${it.timestampMs}_${it.message}" }) { entry ->
                            LogItemRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: FtpLogEntry) {
    val tagColor = when (entry.level) {
        FtpLogLevel.INFO -> Cyan
        FtpLogLevel.TRANSFER -> Color(0xFF34D399) // Emerald
        FtpLogLevel.AUTH -> Violet
        FtpLogLevel.WARNING -> Color(0xFFFBBF24) // Amber
        FtpLogLevel.ERROR -> Rose
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.formattedTime,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "[${entry.level.name}]",
            style = MaterialTheme.typography.bodySmall,
            color = tagColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun LiveActivityConsolePreview() {
    dev.pritam.host.ui.theme.AppTheme {
        LiveActivityConsole(
            logs = listOf(
                FtpLogEntry(
                    timestampMs = System.currentTimeMillis() - 4000,
                    level = FtpLogLevel.INFO,
                    message = "FTP Server listening on port 2121"
                ),
                FtpLogEntry(
                    timestampMs = System.currentTimeMillis() - 2000,
                    level = FtpLogLevel.AUTH,
                    message = "User 'pritam' authenticated from 192.168.1.142"
                ),
                FtpLogEntry(
                    timestampMs = System.currentTimeMillis() - 500,
                    level = FtpLogLevel.TRANSFER,
                    message = "Uploaded 4.2 MB: /Downloads/dataset.json"
                )
            )
        )
    }
}


