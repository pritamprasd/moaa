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
import dev.motherofallapps.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.logging.ToolLog
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
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SYSTEM LOGS",
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
                    containerColor = Color.Transparent
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
                colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Quick Operation & Unknown Type Preset Chips
            val quickPresets = listOf(
                "All" to "",
                "📁 File Ops" to "FILE OPERATION",
                "📡 NFC Ops" to "NFC OPERATION",
                "📋 Clipboard" to "CLIPBOARD OPERATION",
                "⚠️ Unknown Types" to "UNKNOWN DATA TYPE"
            )
            val presetScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPresets.forEach { (label, query) ->
                    val isSelected = filterState.searchQuery == query || (query.isEmpty() && filterState.searchQuery.isEmpty())
                    val chipColor = when {
                        label.contains("Unknown") -> Color(0xFFF59E0B)
                        label.contains("File") -> Cyan
                        label.contains("NFC") -> Violet
                        label.contains("Clipboard") -> Color(0xFFFBBF24)
                        else -> TextSecondary
                    }
                    FilterChipBadge(
                        label = label,
                        isSelected = isSelected,
                        color = chipColor,
                        onClick = { viewModel.setSearchQuery(query) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Multiselect Dropdown Filter Controls (Levels & Tools)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Levels Multiselect Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    LogLevelsMultiselectDropdown(
                        selectedLevels = filterState.selectedLevels,
                        onToggleLevel = { viewModel.toggleLevel(it) },
                        onSelectAll = { viewModel.selectAllLevels() },
                        onClearAll = { viewModel.clearLevelFilters() }
                    )
                }

                // Tools Multiselect Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    ToolsMultiselectDropdown(
                        availableTools = viewModel.getAvailableTools(),
                        selectedToolIds = filterState.selectedToolIds,
                        onToggleTool = { viewModel.toggleTool(it) },
                        onClearAll = { viewModel.clearToolFilters() }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. Log Console List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDeep)
                        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(12.dp))
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
private fun LogLevelsMultiselectDropdown(
    selectedLevels: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val allLevels = LogLevel.entries

    val buttonLabel = when {
        selectedLevels.isEmpty() -> "Levels: All"
        selectedLevels.size == allLevels.size -> "Levels: All"
        else -> "Levels: ${selectedLevels.size} Active"
    }

    val shape = RoundedCornerShape(10.dp)

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { isMenuExpanded = true }
                .border(
                    BorderStroke(1.dp, if (selectedLevels.isNotEmpty()) Cyan else GlassBorder),
                    shape
                ),
            color = if (selectedLevels.isNotEmpty()) Cyan.copy(alpha = 0.12f) else GlassSurfaceDeep,
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedLevels.isNotEmpty()) Cyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = if (isMenuExpanded) "▲" else "▼",
                    color = if (selectedLevels.isNotEmpty()) Cyan else TextSecondary,
                    fontSize = 9.sp
                )
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier
                .background(GlassSurfaceElevated)
                .border(BorderStroke(1.dp, GlassBorderHighlight), RoundedCornerShape(8.dp))
        ) {
            // Quick Actions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onSelectAll,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Select All", color = Cyan, fontSize = 10.sp)
                }
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Clear All", color = Rose, fontSize = 10.sp)
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFF334155))

            allLevels.forEach { level ->
                val isChecked = selectedLevels.isEmpty() || selectedLevels.contains(level)
                val levelColor = when (level) {
                    LogLevel.VERBOSE -> Color(0xFF94A3B8)
                    LogLevel.DEBUG -> Cyan
                    LogLevel.INFO -> Color(0xFF34D399)
                    LogLevel.WARN -> Color(0xFFFBBF24)
                    LogLevel.ERROR -> Rose
                }

                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = levelColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = level.name,
                                    color = levelColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Text(
                            text = if (selectedLevels.contains(level)) "✓" else if (selectedLevels.isEmpty()) "•" else "",
                            color = if (selectedLevels.contains(level)) Cyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { onToggleLevel(level) }
                )
            }
        }
    }
}

@Composable
private fun ToolsMultiselectDropdown(
    availableTools: List<Pair<String, String>>,
    selectedToolIds: Set<String>,
    onToggleTool: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val buttonLabel = when {
        selectedToolIds.isEmpty() -> "Tools: All"
        else -> "Tools: ${selectedToolIds.size} Active"
    }

    val shape = RoundedCornerShape(10.dp)

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { isMenuExpanded = true }
                .border(
                    BorderStroke(1.dp, if (selectedToolIds.isNotEmpty()) Violet else GlassBorder),
                    shape
                ),
            color = if (selectedToolIds.isNotEmpty()) Violet.copy(alpha = 0.12f) else GlassSurfaceDeep,
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedToolIds.isNotEmpty()) Violet else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = if (isMenuExpanded) "▲" else "▼",
                    color = if (selectedToolIds.isNotEmpty()) Violet else TextSecondary,
                    fontSize = 9.sp
                )
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier
                .background(GlassSurfaceElevated)
                .border(BorderStroke(1.dp, GlassBorderHighlight), RoundedCornerShape(8.dp))
        ) {
            // Quick Action Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SELECT TOOLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Clear (All)", color = Violet, fontSize = 10.sp)
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFF334155))

            availableTools.forEach { (toolId, name) ->
                val isSelected = selectedToolIds.contains(toolId)

                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = name,
                            color = if (isSelected) Violet else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = if (isSelected) "✓" else if (selectedToolIds.isEmpty()) "•" else "",
                            color = if (isSelected) Violet else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { onToggleTool(toolId) }
                )
            }
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
                    if (isSelected) color else GlassBorder
                ),
                shape
            ),
        color = if (isSelected) color.copy(alpha = 0.2f) else GlassSurfaceDeep,
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

    val hasUnknownData = log.message.contains("UNKNOWN DATA TYPE") || log.message.contains("[RAW HEX / BINARY]")
    val isFileOp = log.message.contains("FILE OPERATION")
    val isNfcOp = log.message.contains("NFC OPERATION")
    val isClipboardOp = log.message.contains("CLIPBOARD OPERATION")

    val levelColor = when (log.level) {
        LogLevel.VERBOSE -> Color(0xFF94A3B8)
        LogLevel.DEBUG -> Cyan
        LogLevel.INFO -> Color(0xFF34D399)
        LogLevel.WARN -> Color(0xFFFBBF24)
        LogLevel.ERROR -> Rose
    }

    val cardBorderColor = when {
        log.level == LogLevel.ERROR -> Rose.copy(alpha = 0.6f)
        hasUnknownData -> Color(0xFFF59E0B).copy(alpha = 0.8f)
        isFileOp -> Cyan.copy(alpha = 0.5f)
        isNfcOp -> Violet.copy(alpha = 0.5f)
        isClipboardOp -> Color(0xFFFBBF24).copy(alpha = 0.5f)
        else -> GlassBorder
    }

    val cardBgColor = when {
        hasUnknownData -> Color(0x55451A03)
        else -> GlassSurfaceElevated
    }

    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { isExpanded = !isExpanded }
            .border(BorderStroke(1.dp, cardBorderColor), shape),
        color = cardBgColor,
        shape = shape
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                    // Unknown Data Type Highlighting Badge
                    if (hasUnknownData) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "⚠️ UNKNOWN TYPE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isFileOp) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Cyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FILE OP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isNfcOp) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Violet.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "NFC OP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Violet,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isClipboardOp) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFBBF24).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CLIPBOARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

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

            Spacer(Modifier.height(8.dp))

            // Formatted Structured Message
            FormattedLogContent(message = log.message)

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

@Composable
private fun FormattedLogContent(message: String) {
    val lines = message.lines()

    if (lines.size == 1) {
        SingleLineFormattedLog(text = lines[0])
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            lines.forEach { line ->
                when {
                    line.contains("[UNKNOWN DATA TYPE]") -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF451A03).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = line,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    line.contains("[RAW HEX / BINARY]") -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF451A03).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = line,
                                color = Color(0xFFF59E0B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    line.startsWith("NFC OPERATION") || line.startsWith("FILE OPERATION") || line.startsWith("CLIPBOARD OPERATION") -> {
                        SingleLineFormattedLog(text = line, isHeader = true)
                    }
                    else -> {
                        val color = when {
                            line.contains("[TEXT DATA]") -> Cyan
                            line.contains("[URI / LINK]") -> Color(0xFF60A5FA)
                            line.contains("[WI-FI DATA]") -> Color(0xFF34D399)
                            line.contains("[VCARD CONTACT]") -> Violet
                            line.contains("[APP LAUNCHER]") -> Color(0xFFFB923C)
                            line.contains("[MIME DATA]") -> Color(0xFFE879F9)
                            line.contains("  └─") || line.contains("  ├─") -> Color(0xFFCBD5E1)
                            else -> TextPrimary
                        }
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleLineFormattedLog(text: String, isHeader: Boolean = false) {
    val isUnknown = text.contains("UNKNOWN DATA TYPE") || text.contains("[RAW HEX / BINARY]")
    val isDownload = text.contains("[DOWNLOAD]")
    val isUpload = text.contains("[UPLOAD]")
    val isDelete = text.contains("[DELETE]") || text.contains("[RMDDIR]")
    val isMove = text.contains("[MOVE/RENAME]")
    val isMkdir = text.contains("[MKDIR]")
    val isScan = text.contains("[TAG SCAN]")
    val isWrite = text.contains("[TAG WRITE]")
    val isClipboard = text.contains("CLIPBOARD OPERATION")

    val textColor = when {
        isUnknown -> Color(0xFFFBBF24)
        isDownload -> Cyan
        isUpload -> Color(0xFF34D399)
        isDelete -> Rose
        isMove -> Violet
        isMkdir -> Color(0xFF818CF8)
        isScan -> Color(0xFFE879F9)
        isWrite -> Cyan
        isClipboard -> Color(0xFFFDE047)
        else -> TextPrimary
    }

    val fontWeight = if (isHeader || isUnknown || isScan || isWrite || isDownload || isUpload) FontWeight.SemiBold else FontWeight.Normal

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = textColor,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    AppLogHub.logClipboardOperation(
        toolId = "log-viewer",
        toolName = "System Logs",
        operationType = "COPY",
        label = label,
        content = text
    )
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
