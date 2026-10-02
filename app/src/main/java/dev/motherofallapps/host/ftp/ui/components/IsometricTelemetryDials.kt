package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.motherofallapps.host.ftp.model.FtpTelemetry
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.Violet

/**
 * Grid of live isometric telemetry dials displaying speeds, active clients, and transfer volume.
 */
@Composable
fun IsometricTelemetryDials(
    telemetry: FtpTelemetry,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IsometricStatTile(
                label = "Download Speed",
                value = if (isRunning) telemetry.formatSpeed(telemetry.currentDownloadSpeedBps) else "0 B/s",
                accentColor = Cyan,
                modifier = Modifier.weight(1f)
            )
            IsometricStatTile(
                label = "Upload Speed",
                value = if (isRunning) telemetry.formatSpeed(telemetry.currentUploadSpeedBps) else "0 B/s",
                accentColor = Rose,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IsometricStatTile(
                label = "Active Clients",
                value = if (isRunning) "${telemetry.activeClientsCount}" else "0",
                unit = if (telemetry.activeClientsCount == 1) "device" else "devices",
                accentColor = Violet,
                modifier = Modifier.weight(1f)
            )
            IsometricStatTile(
                label = "Total Transferred",
                value = telemetry.formatBytes(telemetry.totalBytesDownloaded + telemetry.totalBytesUploaded),
                accentColor = Cyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
