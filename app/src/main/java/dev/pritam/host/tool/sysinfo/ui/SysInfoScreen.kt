package dev.pritam.host.tool.sysinfo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.settings.AppSettingsManager
import dev.pritam.host.tool.sysinfo.model.RefreshInterval
import dev.pritam.host.tool.sysinfo.model.SysInfoGroup
import dev.pritam.host.tool.sysinfo.model.SysInfoItem
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.AppTheme
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurface
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.TextTertiary
import dev.pritam.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SysInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: SysInfoViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report by viewModel.report.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val refreshInterval by viewModel.refreshInterval.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val expandedGroupIds by viewModel.expandedGroupIds.collectAsStateWithLifecycle()

    var showIntervalDropdown by remember { mutableStateOf(false) }

    // Filter groups based on search query
    val filteredGroups = remember(report, searchQuery) {
        if (searchQuery.isBlank()) {
            report.groups
        } else {
            report.groups.mapNotNull { group ->
                val matchingItems = group.items.filter { item ->
                    item.key.contains(searchQuery, ignoreCase = true) ||
                            item.value.contains(searchQuery, ignoreCase = true) ||
                            (item.subtitle?.contains(searchQuery, ignoreCase = true) == true)
                }
                if (matchingItems.isNotEmpty() || group.title.contains(searchQuery, ignoreCase = true)) {
                    group.copy(items = if (matchingItems.isNotEmpty()) matchingItems else group.items)
                } else {
                    null
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SYSTEM INFO & SPECS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Cyan,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = { GlassBackButton(onClick = onNavigateBack) },
                actions = {
                    // Copy Full Report Button
                    IconButton(onClick = { viewModel.copyFullReport(context) }) {
                        Text("📋", fontSize = 14.sp)
                    }

                    // Manual Refresh Button
                    IconButton(onClick = { viewModel.refreshNow() }) {
                        Text(if (isRefreshing) "⏳" else "🔄", fontSize = 14.sp)
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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── TOP CONTROLS & AUTO-REFRESH SELECTOR ────────────────────────
            item {
                SystemInfoControlsCard(
                    refreshInterval = refreshInterval,
                    showDropdown = showIntervalDropdown,
                    onToggleDropdown = { showIntervalDropdown = !showIntervalDropdown },
                    onSelectInterval = {
                        viewModel.setRefreshInterval(it)
                        showIntervalDropdown = false
                    },
                    onExpandAll = { viewModel.expandAll() },
                    onCollapseAll = { viewModel.collapseAll() }
                )
            }

            // ── SEARCH & FILTER BAR ────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            text = "Search system specs (e.g. ip, ram, model, mac, kernel)...",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
            }

            // ── KPI SUMMARY TILES ROW ──────────────────────────────────────
            item {
                KpiTelemetrySummaryRow(report = report)
            }

            // ── COLLAPSIBLE SYSTEM INFO GROUPS ─────────────────────────────
            items(filteredGroups, key = { it.id }) { group ->
                val isExpanded = expandedGroupIds.contains(group.id)
                SystemInfoGroupCard(
                    group = group,
                    isExpanded = isExpanded,
                    onToggleExpand = { viewModel.toggleGroupExpansion(group.id) },
                    onCopyGroup = { viewModel.copyGroup(context, group) },
                    onCopyItem = { item -> viewModel.copyItem(context, item) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SystemInfoControlsCard(
    refreshInterval: RefreshInterval,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onSelectInterval: (RefreshInterval) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-refresh interval dropdown anchor
            Box {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Cyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleDropdown() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⏱️", fontSize = 11.sp)
                        Text(
                            text = "Auto-Refresh: ${refreshInterval.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text("▼", fontSize = 8.sp, color = Cyan)
                    }
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = onToggleDropdown,
                    modifier = Modifier
                        .background(AppSettingsManager.getDialogSurfaceColor(98))
                        .border(BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                ) {
                    RefreshInterval.entries.forEach { interval ->
                        val isSelected = refreshInterval == interval
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = interval.displayName,
                                    color = if (isSelected) Cyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = { onSelectInterval(interval) }
                        )
                    }
                }
            }

            // Expand / Collapse All buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GlassSurfaceElevated,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onExpandAll() }
                ) {
                    Text(
                        text = "Expand All",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GlassSurfaceElevated,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onCollapseAll() }
                ) {
                    Text(
                        text = "Collapse All",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiTelemetrySummaryRow(report: dev.pritam.host.tool.sysinfo.model.CompleteSystemReport) {
    val osGroup = report.groups.firstOrNull { it.id == "os_build" }
    val memGroup = report.groups.firstOrNull { it.id == "memory_ram" }
    val battGroup = report.groups.firstOrNull { it.id == "battery_power" }
    val netGroup = report.groups.firstOrNull { it.id == "network_connectivity" }

    val osVer = osGroup?.items?.firstOrNull { it.key == "Android Version" }?.value ?: "Android"
    val ramUtil = memGroup?.items?.firstOrNull { it.key == "RAM Utilization" }?.value?.substringBefore("(") ?: "RAM"
    val battLevel = battGroup?.items?.firstOrNull { it.key == "Battery Level" }?.value?.substringBefore("(") ?: "Battery"
    val netType = netGroup?.items?.firstOrNull { it.key == "Active Network Connection" }?.value ?: "Network"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            Triple("🤖 OS", osVer, Cyan),
            Triple("⚡ RAM", ramUtil, Emerald),
            Triple("🔋 Power", battLevel, Amber),
            Triple("🌐 Net", netType.substringBefore(" Network"), Violet)
        ).forEach { (title, value, color) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemInfoGroupCard(
    group: SysInfoGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyGroup: () -> Unit,
    onCopyItem: (SysInfoItem) -> Unit
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chevron_${group.id}"
    )

    IsometricCard(glowColor = group.accentColor) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(group.emoji, fontSize = 16.sp)
                    Column {
                        Text(
                            text = group.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = group.accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${group.items.size} specifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy Group Button
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = group.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, group.accentColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCopyGroup() }
                    ) {
                        Text(
                            text = "📋 Copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = group.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Chevron
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = group.accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "▼",
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .rotate(chevronAngle),
                            color = group.accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expanded Item List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    group.items.forEach { item ->
                        SysInfoRowItem(item = item, accentColor = group.accentColor, onCopy = { onCopyItem(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SysInfoRowItem(
    item: SysInfoItem,
    accentColor: Color,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (item.isHighlighted) accentColor.copy(alpha = 0.3f) else GlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCopy() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isHighlighted) accentColor else TextSecondary,
                        fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    )

                    Spacer(Modifier.height(1.dp))

                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (item.value.contains(":") || item.value.contains(".") || item.value.contains("/")) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    if (item.subtitle != null) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("📋", fontSize = 10.sp)
                }
            }

            // Progress bar for RAM / Storage / Battery
            if (item.progressFraction != null) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = item.progressColor ?: accentColor,
                    trackColor = GlassSurfaceElevated
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun SysInfoScreenPreview() {
    AppTheme {
        SysInfoScreen(onNavigateBack = {})
    }
}
