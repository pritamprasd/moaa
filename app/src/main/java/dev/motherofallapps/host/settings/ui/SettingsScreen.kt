package dev.motherofallapps.host.settings.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.motherofallapps.host.ftp.ui.components.IsometricCard
import dev.motherofallapps.host.ftp.ui.components.LiquidGlassButton
import dev.motherofallapps.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.settings.AppSettingsManager
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.GlassBorder
import dev.motherofallapps.host.ui.theme.GlassBorderHighlight
import dev.motherofallapps.host.ui.theme.GlassSurface
import dev.motherofallapps.host.ui.theme.GlassSurfaceDeep
import dev.motherofallapps.host.ui.theme.GlassSurfaceElevated
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLogViewer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentColumns by AppSettingsManager.galleryColumnCount.collectAsStateWithLifecycle()

    val currentRetention by AppSettingsManager.logRetentionPolicy.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "APP SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", color = Cyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tool Gallery Layout Card
            item {
                GalleryLayoutSettingsCard(
                    currentColumns = currentColumns,
                    onSelectColumns = {
                        AppSettingsManager.setGalleryColumnCount(it)
                        Toast.makeText(context, "Gallery layout set to $it column${if (it > 1) "s" else ""}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. Diagnostics & Telemetry Card (With Auto-Delete Policy)
            item {
                DiagnosticsSettingsCard(
                    currentRetention = currentRetention,
                    onSelectRetention = { policy ->
                        AppSettingsManager.setLogRetentionPolicy(policy)
                        val purged = AppLogHub.pruneExpiredLogs(policy)
                        val msg = if (purged > 0) {
                            "Retention: ${policy.displayName} ($purged old logs purged)"
                        } else {
                            "Retention: ${policy.displayName}"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onOpenLogViewer = onOpenLogViewer,
                    onClearLogs = {
                        AppLogHub.clear()
                        Toast.makeText(context, "Diagnostic logs cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. LLM Gateway Server Card
            item {
                LlmGatewaySettingsCard(context = context)
            }

            // 4. Homescreen Shortcuts Card
            item {
                HomescreenShortcutsCard(context = context)
            }

            // 5. About & Version Info Card
            item {
                AboutInfoCard(
                    onResetDefaults = {
                        AppSettingsManager.resetToDefaults()
                        Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GalleryLayoutSettingsCard(
    currentColumns: Int,
    onSelectColumns: (Int) -> Unit,
) {
    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HOME GALLERY LAYOUT",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose how many columns to display on the dashboard tool grid",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

            val options = listOf(
                1 to ("1 Column" to "Full-width detailed list with expanded descriptions"),
                2 to ("2 Columns" to "Isometric dual card grid (Balanced Default)"),
                3 to ("3 Columns" to "Matrix multi-deck grid for quick navigation"),
                4 to ("4 Columns" to "Micro-deck dense tiles for compact overview")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (cols, info) ->
                    val isSelected = currentColumns == cols
                    val shape = RoundedCornerShape(10.dp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable { onSelectColumns(cols) }
                            .border(
                                BorderStroke(1.dp, if (isSelected) Cyan else GlassBorder),
                                shape
                            ),
                        color = if (isSelected) Cyan.copy(alpha = 0.15f) else GlassSurfaceDeep,
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = CircleShape,
                                color = if (isSelected) Cyan else Color(0xFF475569)
                            ) {}

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.first,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Cyan else TextPrimary
                                )
                                Text(
                                    text = info.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsSettingsCard(
    currentRetention: dev.motherofallapps.host.settings.LogRetentionPolicy,
    onSelectRetention: (dev.motherofallapps.host.settings.LogRetentionPolicy) -> Unit,
    onOpenLogViewer: () -> Unit,
    onClearLogs: () -> Unit,
) {
    IsometricCard(glowColor = Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM DIAGNOSTICS & LOGGING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Rose,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Rose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "RETENTION: ${currentRetention.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Manage real-time telemetry, auto-delete policy, and memory cleanup.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

            // Auto-delete / retention policy section
            Text(
                text = "AUTO-DELETE OLD LOGS (FREE MEMORY)",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            val policies = dev.motherofallapps.host.settings.LogRetentionPolicy.entries
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                policies.forEach { policy ->
                    val isSelected = currentRetention == policy
                    val shape = RoundedCornerShape(8.dp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable { onSelectRetention(policy) }
                            .border(
                                BorderStroke(1.dp, if (isSelected) Rose else GlassBorder),
                                shape
                            ),
                        color = if (isSelected) Rose.copy(alpha = 0.15f) else GlassSurfaceDeep,
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = CircleShape,
                                color = if (isSelected) Rose else Color(0xFF475569)
                            ) {}

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = policy.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) Rose else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = policy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidGlassButton(
                    onClick = onOpenLogViewer,
                    modifier = Modifier.weight(1f),
                    glowColor = Violet,
                    text = "Open System Logs"
                )

                LiquidGlassButton(
                    onClick = onClearLogs,
                    modifier = Modifier.weight(1f),
                    glowColor = Rose,
                    text = "Clear Buffer"
                )
            }
        }
    }
}

@Composable
private fun HomescreenShortcutsCard(context: Context) {
    val tools = dev.motherofallapps.host.config.ToolRegistryConfig.INSTALLED_TOOLS

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOMESCREEN SHORTCUTS (PIN TO LAUNCHER)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pin dedicated direct-launch icons to your home screen. Tapping an icon opens that tool directly, bypassing the dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tools.forEach { tool ->
                    val accentColor = Color(tool.accentColorHex)
                    val shape = RoundedCornerShape(8.dp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .border(BorderStroke(1.dp, GlassBorder), shape),
                        color = GlassSurfaceDeep,
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accentColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = tool.category.uppercase(),
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = tool.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tool.shortTagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            LiquidGlassButton(
                                onClick = {
                                    dev.motherofallapps.host.shortcut.ShortcutUtils.pinToolToHomeScreen(context, tool)
                                },
                                glowColor = accentColor,
                                useRainbowBorder = true,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("📌 Pin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LlmGatewaySettingsCard(context: Context) {
    val httpServer = dev.motherofallapps.host.tool.llmgateway.manager.LlmGatewayManager.getHttpServer(context)
    val telemetry by httpServer.telemetry.collectAsStateWithLifecycle()

    IsometricCard(glowColor = if (telemetry.isRunning) Color(0xFF34D399) else Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL LLM GATEWAY SERVER",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (telemetry.isRunning) Color(0xFF34D399) else Rose,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (telemetry.isRunning) Color(0xFF34D399).copy(alpha = 0.15f) else Rose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (telemetry.isRunning) "● RUNNING :8080" else "○ STOPPED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetry.isRunning) Color(0xFF34D399) else Rose,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Embedded local HTTP loopback server (http://127.0.0.1:8080) for container tools and local apps with OpenAI-compatible API spec, multi-account pooling & failover.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

            LiquidGlassButton(
                onClick = {
                    if (telemetry.isRunning) {
                        httpServer.stop()
                        Toast.makeText(context, "LLM Gateway Server stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        httpServer.start()
                        Toast.makeText(context, "LLM Gateway Server started on :8080", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                glowColor = if (telemetry.isRunning) Rose else Color(0xFF34D399),
                useRainbowBorder = !telemetry.isRunning,
                text = if (telemetry.isRunning) "Stop Gateway Server" else "Start Gateway Server"
            )
        }
    }
}

@Composable
private fun AboutInfoCard(
    onResetDefaults: () -> Unit,
) {
    IsometricCard(glowColor = Color(0xFF475569)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ABOUT MOTHER OF ALL APPS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "MotherOfAllApps v0.1.0 · Android 14+ (API 34..37)\n" +
                        "Architecture: Jetpack Compose + Modular Tool Plugins\n" +
                        "Design Identity: Cyberpunk Dark Isometric UI",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(14.dp))

            LiquidGlassButton(
                onClick = onResetDefaults,
                modifier = Modifier.fillMaxWidth(),
                glowColor = Color(0xFF94A3B8),
                text = "Reset App Preferences to Defaults"
            )
        }
    }
}

