package dev.motherofallapps.host.tool.logviewer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.motherofallapps.host.ftp.ui.components.IsometricCard
import dev.motherofallapps.host.ftp.ui.components.IsometricStatTile
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.logging.ToolLog
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogViewerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Auto-scroll to top when new logs arrive (since logs are latest-first)
    LaunchedEffect(filteredLogs.size, filterState.isAutoScroll) {
        if (filterState.isAutoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "TOOL LOG VIEWER",
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
                actions = {
                    TextButton(
                        onClick = {
                            val export = viewModel.exportFilteredLogsToString()
                            copyToClipboard(context, "All Filtered Logs", export)
                        }
                    ) {
                        Text("Export", color = Cyan, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.clearLogs() }) {
                        Text("Clear", color = Rose, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Stats Telemetry Banner
            IsometricLogStatsBar(
                stats = stats,
                isAutoScroll = filterState.isAutoScroll,
                onToggleAutoScroll = { viewModel.toggleAutoScroll() }
            )

            Spacer(Modifier.height(12.dp))

            // 2. Search & Filter Bar
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Filter logs by message, tag, or tool...", fontSize = 12.sp) },
                singleLine = true,
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        TextButton(onClick = { viewModel.setSearchQuery("") }) {
                            Text("Clear", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // 3. Log Level Selector Filter Chips
            LogLevelFilterRow(
                selectedLevel = filterState.selectedLevel,
                onSelectLevel = { viewModel.setLevelFilter(it) }
            )

            Spacer(Modifier.height(8.dp))

            // 4. Tool Filter Chips
            ToolFilterRow(
                selectedToolId = filterState.selectedToolId,
                onSelectTool = { viewModel.setToolFilter(it) }
            )

            Spacer(Modifier.height(12.dp))

            // 5. Log Console List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDeep)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No log events match the current filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Logs from all tools will automatically appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredLogs, key = { it.id }) { logEntry ->
                        LogViewerEntryCard(
                            log = logEntry,
                            onCopy = { copyToClipboard(context, "Log Entry", "[${it.formattedTime}] [${it.toolName}/${it.tag}] ${it.message}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IsometricLogStatsBar(
    stats: LogViewerStats,
    isAutoScroll: Boolean,
    onToggleAutoScroll: () -> Unit,
) {
    IsometricCard(glowColor = Violet, elevationDepth = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("TOTAL LOGS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Text("${stats.totalCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("MATCHES", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 9.sp)
                    Text("${stats.matchCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Cyan)
                }
                Column {
                    Text("ERRORS", style = MaterialTheme.typography.labelSmall, color = Rose, fontSize = 9.sp)
                    Text("${stats.errorCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (stats.errorCount > 0) Rose else TextSecondary)
                }
            }

            OutlinedButton(
                onClick = onToggleAutoScroll,
                border = BorderStroke(1.dp, if (isAutoScroll) Cyan else Color(0xFF334155)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isAutoScroll) Cyan else TextSecondary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(if (isAutoScroll) "● Live Stream" else "○ Paused", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun LogLevelFilterRow(
    selectedLevel: LogLevel?,
    onSelectLevel: (LogLevel?) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChipBadge(
            label = "ALL LEVELS",
            isSelected = selectedLevel == null,
            color = TextPrimary,
            onClick = { onSelectLevel(null) }
        )
        LogLevel.entries.forEach { level ->
            val color = when (level) {
                LogLevel.VERBOSE -> Color(0xFF94A3B8)
                LogLevel.DEBUG -> Cyan
                LogLevel.INFO -> Color(0xFF34D399)
                LogLevel.WARN -> Color(0xFFFBBF24)
                LogLevel.ERROR -> Rose
            }
            FilterChipBadge(
                label = level.name,
                isSelected = selectedLevel == level,
                color = color,
                onClick = { onSelectLevel(level) }
            )
        }
    }
}

@Composable
private fun ToolFilterRow(
    selectedToolId: String?,
    onSelectTool: (String?) -> Unit,
) {
    val scrollState = rememberScrollState()
    val knownTools = listOf(
        null to "ALL TOOLS",
        "ftp-server" to "LAN FTP Server",
        "host-system" to "System Core"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        knownTools.forEach { (toolId, label) ->
            FilterChipBadge(
                label = label.uppercase(),
                isSelected = selectedToolId == toolId,
                color = Violet,
                onClick = { onSelectTool(toolId) }
            )
        }
    }
}

@Composable
private fun FilterChipBadge(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable { onClick() }
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) color else Color(0xFF334155)
                ),
                shape
            ),
        color = if (isSelected) color.copy(alpha = 0.2f) else SurfaceDeep,
        shape = shape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun LogViewerEntryCard(
    log: ToolLog,
    onCopy: (ToolLog) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val levelColor = when (log.level) {
        LogLevel.VERBOSE -> Color(0xFF94A3B8)
        LogLevel.DEBUG -> Cyan
        LogLevel.INFO -> Color(0xFF34D399)
        LogLevel.WARN -> Color(0xFFFBBF24)
        LogLevel.ERROR -> Rose
    }

    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { isExpanded = !isExpanded }
            .border(
                BorderStroke(
                    1.dp,
                    if (log.level == LogLevel.ERROR) Rose.copy(alpha = 0.5f) else Color(0xFF1E293B)
                ),
                shape
            ),
        color = SurfaceElevated.copy(alpha = 0.9f),
        shape = shape
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = log.level.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "${log.toolName} · ${log.tag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = log.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (!log.stackTrace.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = log.stackTrace,
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onCopy(log) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Copy Entry", fontSize = 10.sp, color = Cyan)
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
