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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ftp.storage.StorageUtils
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@Composable
fun StorageInfoCard(
    currentPath: String,
    storageInfo: StorageUtils.StorageInfo,
    hasStoragePermission: Boolean,
    isRunning: Boolean,
    onChangeFolder: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsometricCard(
        modifier = modifier,
        glowColor = if (!hasStoragePermission) Rose else Cyan
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVED STORAGE ROOT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!hasStoragePermission) Rose else Cyan,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )

                OutlinedButton(
                    onClick = onChangeFolder,
                    enabled = !isRunning,
                    border = BorderStroke(1.dp, if (!isRunning) Cyan.copy(alpha = 0.5f) else Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (!isRunning) Cyan else TextSecondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Change", fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Current directory path badge
            val pathShape = RoundedCornerShape(8.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pathShape),
                color = SurfaceDeep,
                shape = pathShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Storage capacity bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Free: ${StorageUtils.formatBytes(storageInfo.freeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Total: ${StorageUtils.formatBytes(storageInfo.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { storageInfo.usedSpacePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Cyan,
                trackColor = SurfaceDeep,
                strokeCap = StrokeCap.Round,
            )

            // Permission alert if needed
            if (!hasStoragePermission) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRequestPermission() },
                    color = Rose.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ All Files Access required to serve storage. Tap to grant permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun StorageInfoCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        StorageInfoCard(
            currentPath = "/storage/emulated/0/Downloads",
            storageInfo = StorageUtils.StorageInfo(
                rootPath = "/storage/emulated/0/Downloads",
                totalBytes = 128L * 1024 * 1024 * 1024,
                freeBytes = 48L * 1024 * 1024 * 1024,
                usedBytes = 80L * 1024 * 1024 * 1024
            ),
            hasStoragePermission = true,
            isRunning = false,
            onChangeFolder = {},
            onRequestPermission = {}
        )
    }
}


