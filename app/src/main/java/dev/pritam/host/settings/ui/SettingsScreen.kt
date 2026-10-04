package dev.pritam.host.settings.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pritam.ghostagent.GhostAgentManager
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.settings.AccentPalette
import dev.pritam.host.settings.AppSettingsManager
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.TextTertiary
import dev.pritam.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenSystemManual: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentColumns by AppSettingsManager.galleryColumnCount.collectAsStateWithLifecycle()
    val currentPalette by AppSettingsManager.accentPalette.collectAsStateWithLifecycle()
    val customPalettes by AppSettingsManager.customPalettes.collectAsStateWithLifecycle()
    val currentPadding by AppSettingsManager.dashboardPaddingDp.collectAsStateWithLifecycle()
    val currentRetention by AppSettingsManager.logRetentionPolicy.collectAsStateWithLifecycle()

    // ── Row inline expansion states ──────────────────────────────────────────
    var accentExpanded by remember { mutableStateOf(false) }
    var galleryExpanded by remember { mutableStateOf(false) }
    var paddingExpanded by remember { mutableStateOf(false) }
    var openSourceExpanded by remember { mutableStateOf(false) }
    var shortcutsExpanded by remember { mutableStateOf(false) }
    var llmExpanded by remember { mutableStateOf(false) }
    var mcpExpanded by remember { mutableStateOf(false) }
    var ghostExpanded by remember { mutableStateOf(false) }
    var retentionMenuExpanded by remember { mutableStateOf(false) }

    // Dialog state for info dialogs
    var activeInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreatePaletteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090A0E),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(onClick = onNavigateBack)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    fontSize = 20.sp,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)
        ) {

            // ═════════════════════════════════════════════════════════════════
            // 1. DISPLAY & THEME
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_display") {
                SettingsSectionTitle(title = "Display & Theme")
            }

            item(key = "group_display") {
                SettingsGroupCard {
                    // Row 1: Accent Color Theme
                    SettingsRowItem(
                        icon = Icons.Outlined.Palette,
                        title = "Accent Color Theme",
                        subtitle = currentPalette.displayName,
                        iconTint = currentPalette.primary,
                        iconBackgroundColor = currentPalette.primary.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Accent Color Theme" to
                                "Choose from 5 curated minimalist dark palettes tuned for high contrast and minimal glare, or tap (+) to create custom tri-color combinations with instant system-wide application."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.primary))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.secondary))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.tertiary))
                                }
                                SettingChevron(isExpanded = accentExpanded)
                            }
                        },
                        onClick = { accentExpanded = !accentExpanded }
                    )

                    // Inline expand: Accent Theme Selector & Custom Creator
                    AnimatedVisibility(
                        visible = accentExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            var dropdownMenuExpanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dropdown selector trigger
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        onClick = { dropdownMenuExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF161822),
                                        border = BorderStroke(1.dp, Color(0xFF242735)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.primary))
                                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.secondary))
                                                    Box(Modifier.size(10.dp).clip(CircleShape).background(currentPalette.tertiary))
                                                }
                                                Text(
                                                    text = currentPalette.displayName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFF1F5F9),
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = dropdownMenuExpanded,
                                        onDismissRequest = { dropdownMenuExpanded = false },
                                        modifier = Modifier
                                            .background(Color(0xFF14161F))
                                            .border(1.dp, Color(0xFF222531), RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "PRESETS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )

                                        AccentPalette.builtInPalettes.forEach { palette ->
                                            val isSelected = palette.id == currentPalette.id
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.primary))
                                                            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.secondary))
                                                            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.tertiary))
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = palette.displayName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) palette.primary else TextPrimary,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                },
                                                trailingIcon = {
                                                    if (isSelected) {
                                                        Text(
                                                            text = "✓",
                                                            color = palette.primary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    AppSettingsManager.setAccentPalette(palette)
                                                    dropdownMenuExpanded = false
                                                    Toast.makeText(context, "Accent set to ${palette.displayName}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }

                                        if (customPalettes.isNotEmpty()) {
                                            HorizontalDivider(
                                                color = Color(0xFF222531),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                            Text(
                                                text = "CUSTOM COMBINATIONS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = currentPalette.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )

                                            customPalettes.forEach { palette ->
                                                val isSelected = palette.id == currentPalette.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(palette.primary))
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(palette.secondary))
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(palette.tertiary))
                                                            }
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(
                                                                text = palette.displayName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) palette.primary else TextPrimary,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    },
                                                    trailingIcon = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (isSelected) {
                                                                Text("✓", color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                Spacer(Modifier.width(8.dp))
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    AppSettingsManager.deleteCustomPalette(palette.id)
                                                                    Toast.makeText(context, "Custom palette removed", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Delete Custom Palette",
                                                                    tint = TextSecondary.copy(alpha = 0.6f),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        AppSettingsManager.setAccentPalette(palette)
                                                        dropdownMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // (+) Button to create custom palette
                                Surface(
                                    onClick = { showCreatePaletteDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF161822),
                                    border = BorderStroke(1.dp, currentPalette.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Create Custom Accent",
                                            tint = currentPalette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 2: Gallery Columns
                    SettingsRowItem(
                        icon = Icons.Outlined.GridView,
                        title = "Gallery Columns",
                        subtitle = "$currentColumns Column${if (currentColumns > 1) "s" else ""}",
                        iconTint = Cyan,
                        iconBackgroundColor = Cyan.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Gallery Columns" to
                                "Configure how tools are arranged on the home dashboard:\n\n• 1 Column: Full-width detailed list\n• 2 Columns: Dual isometric cards (Balanced Default)\n• 3 Columns: Matrix grid for high density\n• 4 Columns: Micro-deck dense tiles"
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..4).forEach { cols ->
                                    val isSel = currentColumns == cols
                                    Surface(
                                        onClick = {
                                            AppSettingsManager.setGalleryColumnCount(cols)
                                            Toast.makeText(context, "Gallery set to $cols column${if (cols > 1) "s" else ""}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Cyan.copy(alpha = 0.2f) else Color(0xFF1B1D26),
                                        border = BorderStroke(1.dp, if (isSel) Cyan else Color(0xFF262936)),
                                        modifier = Modifier.size(width = 28.dp, height = 26.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$cols",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Cyan else Color(0xFF94A3B8),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(2.dp))
                                SettingChevron(isExpanded = galleryExpanded)
                            }
                        },
                        onClick = { galleryExpanded = !galleryExpanded }
                    )

                    // Inline expand: Gallery layout fine slider
                    AnimatedVisibility(
                        visible = galleryExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            val layoutDescription = when (currentColumns) {
                                1 -> "1 Column: Full-width detailed list with expanded descriptions"
                                2 -> "2 Columns: Dual isometric cards (Balanced Default)"
                                3 -> "3 Columns: Matrix grid for high density & rapid launch"
                                4 -> "4 Columns: Micro-deck dense tiles for compact overview"
                                else -> "$currentColumns Columns Grid"
                            }
                            Text(
                                text = layoutDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Slider(
                                value = currentColumns.toFloat(),
                                onValueChange = { AppSettingsManager.setGalleryColumnCount(it.toInt().coerceIn(1, 4)) },
                                valueRange = 1f..4f,
                                steps = 2,
                                colors = SliderDefaults.colors(
                                    thumbColor = Cyan,
                                    activeTrackColor = Cyan,
                                    inactiveTrackColor = Color(0xFF20222A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    SettingsDivider()

                    // Row 3: Dashboard Padding
                    SettingsRowItem(
                        icon = Icons.Outlined.SpaceDashboard,
                        title = "Dashboard Padding",
                        subtitle = "$currentPadding dp",
                        iconTint = Cyan,
                        iconBackgroundColor = Cyan.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Dashboard Padding" to
                                "Adjust outer screen margins and card spacing across the dashboard. Range: 8dp to 32dp."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = "$currentPadding dp", color = Cyan)
                                SettingChevron(isExpanded = paddingExpanded)
                            }
                        },
                        onClick = { paddingExpanded = !paddingExpanded }
                    )

                    // Inline expand: Dashboard padding slider & quick presets
                    AnimatedVisibility(
                        visible = paddingExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Slider(
                                value = currentPadding.toFloat(),
                                onValueChange = { AppSettingsManager.setDashboardPadding(it.toInt()) },
                                valueRange = 8f..32f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = Cyan,
                                    activeTrackColor = Cyan,
                                    inactiveTrackColor = Color(0xFF20222A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    10 to "Compact (10dp)",
                                    18 to "Default (18dp)",
                                    26 to "Spacious (26dp)"
                                ).forEach { (dpValue, label) ->
                                    val isSel = currentPadding == dpValue
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Cyan.copy(alpha = 0.2f) else Color(0xFF161822),
                                        border = BorderStroke(1.dp, if (isSel) Cyan else Color(0xFF242735)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { AppSettingsManager.setDashboardPadding(dpValue) }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSel) Cyan else Color(0xFF94A3B8),
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }


                }
            }

            // ═════════════════════════════════════════════════════════════════
            // 2. TOOLS & SHORTCUTS
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_tools") {
                SettingsSectionTitle(title = "Tools & Shortcuts")
            }

            item(key = "group_tools") {
                val tools = dev.pritam.host.config.ToolRegistryConfig.INSTALLED_TOOLS

                SettingsGroupCard {
                    // Row 1: Homescreen Shortcuts
                    SettingsRowItem(
                        icon = Icons.Outlined.Apps,
                        title = "Homescreen Shortcuts",
                        subtitle = "Pin 1-tap direct launch shortcuts",
                        iconTint = Cyan,
                        iconBackgroundColor = Cyan.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Homescreen Shortcuts" to
                                "Tap any tool icon below to pin a 1-tap direct launch shortcut to your Android launcher home screen."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = "${tools.size} TOOLS", color = Cyan)
                                SettingChevron(isExpanded = shortcutsExpanded)
                            }
                        },
                        onClick = { shortcutsExpanded = !shortcutsExpanded }
                    )

                    // Inline expand: Homescreen launcher shortcuts grid
                    AnimatedVisibility(
                        visible = shortcutsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val chunkedTools = tools.chunked(3)
                            chunkedTools.forEach { rowTools ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowTools.forEach { tool ->
                                        val accentColor = Color(tool.accentColorHex)
                                        val shape = RoundedCornerShape(10.dp)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(110.dp)
                                                .clip(shape)
                                                .clickable {
                                                    dev.pritam.host.shortcut.ShortcutUtils.pinToolToHomeScreen(context, tool)
                                                }
                                                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), shape),
                                            color = Color(0xFF14161F),
                                            shape = shape
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = accentColor.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = tool.emoji, fontSize = 18.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = tool.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFF1F5F9),
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        lineHeight = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - rowTools.size) {
                                        Spacer(modifier = Modifier.weight(1f).height(110.dp))
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 2: System Manual & Guide
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "System Manual & Guide",
                        subtitle = "Architecture, specs & tool guides",
                        iconTint = Cyan,
                        iconBackgroundColor = Cyan.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "System Manual" to
                                "Interactive system manual, architectural specifications, tool guides, and design guidelines for Mother of All Apps."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = "DOCS", color = Cyan)
                                SettingChevron()
                            }
                        },
                        onClick = onOpenSystemManual
                    )
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // 3. AI & AUTOMATION
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_ai") {
                SettingsSectionTitle(title = "AI & Automation")
            }

            item(key = "group_ai") {
                val repository = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getRepository(context)
                val profiles by repository.profiles.collectAsStateWithLifecycle()
                val defaultProfile = profiles.firstOrNull { it.isEnabled } ?: profiles.firstOrNull()

                val mcpRepository = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getMcpRepository(context)
                val mcpServers by mcpRepository.servers.collectAsStateWithLifecycle()
                val activeToolsCount = mcpServers.filter { it.isEnabled }.sumOf { s -> s.discoveredTools.count { it.isEnabled } }

                val httpServer = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getHttpServer(context)
                val telemetry by httpServer.telemetry.collectAsStateWithLifecycle()

                val isGhostConnected = GhostAgentManager.isAccessibilityServiceConnected()
                val isBubbleVisible = GhostAgentManager.isBubbleVisible()

                SettingsGroupCard {
                    // Row 1: Default LLM Provider
                    SettingsRowItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Default LLM Provider",
                        subtitle = defaultProfile?.name ?: "Configure provider",
                        iconTint = Violet,
                        iconBackgroundColor = Violet.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Default LLM Route" to
                                "Select the primary model used for CyberChat and default requests. If offline or rate-limited, requests fail over to the next priority target in the profile pool."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = defaultProfile?.targetModel ?: "AUTO", color = Violet)
                                SettingChevron(isExpanded = llmExpanded)
                            }
                        },
                        onClick = { llmExpanded = !llmExpanded }
                    )

                    // Inline expand: Default LLM Profile Selection
                    AnimatedVisibility(
                        visible = llmExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profiles.forEach { profile ->
                                val isDefault = profile.id == defaultProfile?.id
                                val surfaceColor = if (isDefault) Violet.copy(alpha = 0.12f) else Color(0xFF14161F)
                                val borderColor = if (isDefault) Violet else Color(0xFF222531)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = surfaceColor,
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isDefault) {
                                                repository.setDefaultProfile(profile.id)
                                                Toast.makeText(context, "Default LLM set to: ${profile.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .border(2.dp, if (isDefault) Violet else Color(0xFF4B5263), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDefault) {
                                                    Box(Modifier.size(8.dp).background(Violet, CircleShape))
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = profile.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isDefault) Violet else Color(0xFFF1F5F9),
                                                        fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp
                                                    )
                                                    if (isDefault) {
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = Violet.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "DEFAULT",
                                                                color = Violet,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${profile.providerType} • ${profile.targetModel ?: "auto"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF6B7280),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 2: MCP Tool Servers
                    SettingsRowItem(
                        icon = Icons.Outlined.Extension,
                        title = "Model Context Protocol (MCP)",
                        subtitle = "$activeToolsCount active tools across ${mcpServers.size} servers",
                        iconTint = Violet,
                        iconBackgroundColor = Violet.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "MCP Tool Servers" to
                                "Turn Model Context Protocol servers on or off. Active tools (hardware sensors, FTP, system logs, remote APIs) are dynamically exposed to LLMs for automated tool calling."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = "${mcpServers.size} SERVERS", color = Violet)
                                SettingChevron(isExpanded = mcpExpanded)
                            }
                        },
                        onClick = { mcpExpanded = !mcpExpanded }
                    )

                    // Inline expand: MCP Servers list with toggle
                    AnimatedVisibility(
                        visible = mcpExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mcpServers.forEach { server ->
                                val isBuiltin = server.transportType == dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType.BUILTIN_DEVICE
                                val enabledTools = server.discoveredTools.count { it.isEnabled }
                                val totalTools = server.discoveredTools.size

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF14161F),
                                    border = BorderStroke(1.dp, if (server.isEnabled) Violet.copy(alpha = 0.4f) else Color(0xFF222531)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = server.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (server.isEnabled) Color(0xFFF1F5F9) else Color(0xFF6B7280),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = if (isBuiltin) Emerald.copy(alpha = 0.15f) else Cyan.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (isBuiltin) "BUILTIN" else server.transportType.name,
                                                        color = if (isBuiltin) Emerald else Cyan,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isBuiltin) "$enabledTools of $totalTools tools active" else "${server.endpointUrl.ifBlank { "No endpoint" }} • $enabledTools tools",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF6B7280),
                                                fontSize = 10.sp
                                            )
                                        }

                                        Switch(
                                            checked = server.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                mcpRepository.toggleServerEnabled(server.id, isChecked)
                                                Toast.makeText(context, "${server.name} ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Violet,
                                                checkedTrackColor = Violet.copy(alpha = 0.35f),
                                                uncheckedThumbColor = Color(0xFF6B7280),
                                                uncheckedTrackColor = Color(0xFF1B1D26)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 3: Local LLM Gateway
                    SettingsRowItem(
                        icon = Icons.Outlined.Dns,
                        title = "Local LLM Gateway Server",
                        subtitle = if (telemetry.isRunning) "Running on :8080 (http://127.0.0.1:8080)" else "Stopped (http://127.0.0.1:8080)",
                        iconTint = if (telemetry.isRunning) Emerald else Rose,
                        iconBackgroundColor = (if (telemetry.isRunning) Emerald else Rose).copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Local LLM Gateway Server" to
                                "Embedded local HTTP loopback server (http://127.0.0.1:8080) for container tools and local apps with OpenAI-compatible API spec, multi-account pooling & failover."
                        },
                        trailingContent = {
                            Switch(
                                checked = telemetry.isRunning,
                                onCheckedChange = { start ->
                                    if (start) {
                                        httpServer.start()
                                        Toast.makeText(context, "LLM Gateway started on :8080", Toast.LENGTH_SHORT).show()
                                    } else {
                                        httpServer.stop()
                                        Toast.makeText(context, "LLM Gateway stopped", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Emerald,
                                    checkedTrackColor = Emerald.copy(alpha = 0.35f),
                                    uncheckedThumbColor = Color(0xFF6B7280),
                                    uncheckedTrackColor = Color(0xFF1B1D26)
                                )
                            )
                        }
                    )

                    SettingsDivider()

                    // Row 4: Ghost Agent
                    SettingsRowItem(
                        icon = Icons.Outlined.SmartToy,
                        title = "Ghost Agent Automation",
                        subtitle = if (isGhostConnected) "Accessibility service active" else "Accessibility permission required",
                        iconTint = if (isGhostConnected) Emerald else Amber,
                        iconBackgroundColor = (if (isGhostConnected) Emerald else Amber).copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Ghost Agent" to
                                "Automates multi-step tasks across any app using Android Accessibility Service. Enable A11y access in system settings to start automation workflows."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(
                                    text = if (isGhostConnected) "ACTIVE" else "DISABLED",
                                    color = if (isGhostConnected) Emerald else Amber
                                )
                                SettingChevron(isExpanded = ghostExpanded)
                            }
                        },
                        onClick = { ghostExpanded = !ghostExpanded }
                    )

                    // Inline expand: Ghost Agent controls
                    AnimatedVisibility(
                        visible = ghostExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF14161F),
                                border = BorderStroke(1.dp, Color(0xFF222531))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Floating Bubble Overlay",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFF1F5F9),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Always-on 👻 overlay over other apps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6B7280),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = isBubbleVisible,
                                        onCheckedChange = { enabled ->
                                            if (enabled) GhostAgentManager.showFloatingBubble(context)
                                            else GhostAgentManager.hideFloatingBubble(context)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Violet,
                                            checkedTrackColor = Violet.copy(alpha = 0.35f),
                                            uncheckedThumbColor = Color(0xFF6B7280),
                                            uncheckedTrackColor = Color(0xFF1B1D26)
                                        )
                                    )
                                }
                            }

                            if (!isGhostConnected) {
                                LiquidGlassButton(
                                    onClick = {
                                        context.startActivity(
                                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    glowColor = Amber,
                                    text = "Enable Accessibility Service →"
                                )
                            }
                        }
                    }
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // 4. PERFORMANCE & MEMORY OPTIMIZATION
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_performance") {
                SettingsSectionTitle(title = "Performance & Memory")
            }

            item(key = "group_performance") {
                var lastSummary by remember { mutableStateOf<dev.pritam.host.optimizer.AppPerformanceOptimizer.OptimizationSummary?>(null) }

                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Speed Up & Optimize App",
                        subtitle = lastSummary?.let {
                            "Freed ${String.format(java.util.Locale.US, "%.1f", it.memoryReclaimedMb)} MB • ${it.sensorsPausedCount} sensors suspended • ${it.logsPrunedCount} logs purged"
                        } ?: "Suspends background sensors, purges logs, and compacts heap memory",
                        iconTint = Emerald,
                        iconBackgroundColor = Emerald.copy(alpha = 0.15f),
                        onInfoClick = {
                            activeInfoDialog = "Performance Booster & Cleaner" to
                                "Instantly makes the app faster by stopping idle hardware sensor streaming listeners, purging excessive in-memory logs down to a minimal baseline, closing stale connection buffers, and reclaiming JVM heap memory without changing any of your saved settings or data."
                        },
                        trailingContent = {
                            Surface(
                                onClick = {
                                    val result = dev.pritam.host.optimizer.AppPerformanceOptimizer.boostPerformance(context)
                                    lastSummary = result
                                    val msg = "⚡ System Boosted: Freed ${String.format(java.util.Locale.US, "%.1f", result.memoryReclaimedMb)} MB RAM (${result.sensorsPausedCount} sensors suspended, ${result.logsPrunedCount} logs pruned)"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Emerald.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("⚡", fontSize = 11.sp)
                                    Text(
                                        text = "Boost Now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // 5. DIAGNOSTICS & LOGS
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_diagnostics") {
                SettingsSectionTitle(title = "Diagnostics & Logs")
            }

            item(key = "group_diagnostics") {
                SettingsGroupCard {
                    // Row 1: System Diagnostic Logs
                    SettingsRowItem(
                        icon = Icons.Outlined.Terminal,
                        title = "Diagnostic Log Hub",
                        subtitle = "Real-time telemetry & trace viewer",
                        iconTint = Color(0xFF38BDF8),
                        iconBackgroundColor = Color(0xFF38BDF8).copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "System Diagnostics & Logging" to
                                "Manage real-time telemetry, auto-delete policy, and memory cleanup. View live diagnostic streams or purge logs to reclaim storage."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(text = "VIEW", color = Color(0xFF38BDF8))
                                SettingChevron()
                            }
                        },
                        onClick = onOpenLogViewer
                    )

                    SettingsDivider()

                    // Row 2: Log Retention Policy
                    SettingsRowItem(
                        icon = Icons.Outlined.History,
                        title = "Log Retention Policy",
                        subtitle = currentRetention.description,
                        iconTint = Amber,
                        iconBackgroundColor = Amber.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Log Retention Policy" to
                                "Configures how long diagnostic logs are kept on device before automatic pruning."
                        },
                        trailingContent = {
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingBadge(text = currentRetention.displayName, color = Amber)
                                    SettingChevron(isExpanded = retentionMenuExpanded)
                                }

                                DropdownMenu(
                                    expanded = retentionMenuExpanded,
                                    onDismissRequest = { retentionMenuExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF14161F))
                                        .border(BorderStroke(1.dp, Color(0xFF222531)), RoundedCornerShape(8.dp))
                                ) {
                                    dev.pritam.host.settings.LogRetentionPolicy.entries.forEach { policy ->
                                        val isSelected = currentRetention == policy
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = policy.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isSelected) Amber else Color(0xFFF1F5F9),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = policy.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF6B7280),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        color = Amber,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            },
                                            onClick = {
                                                retentionMenuExpanded = false
                                                AppSettingsManager.setLogRetentionPolicy(policy)
                                                val purged = AppLogHub.pruneExpiredLogs(policy)
                                                val msg = if (purged > 0) "Retention: ${policy.displayName} ($purged old logs purged)"
                                                else "Retention: ${policy.displayName}"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        onClick = { retentionMenuExpanded = !retentionMenuExpanded }
                    )

                    SettingsDivider()

                    // Row 3: Clear Diagnostic Logs
                    SettingsRowItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "Clear All Logs",
                        subtitle = "Purge in-memory telemetry buffer",
                        iconTint = Rose,
                        iconBackgroundColor = Rose.copy(alpha = 0.12f),
                        trailingContent = {
                            Surface(
                                onClick = {
                                    AppLogHub.clear()
                                    Toast.makeText(context, "Diagnostic logs cleared", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = Rose.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Rose.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Rose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // 6. ABOUT
            // ═════════════════════════════════════════════════════════════════
            item(key = "title_about") {
                SettingsSectionTitle(title = "About")
            }

            item(key = "group_about") {
                SettingsGroupCard {
                    // Row 1: App Info
                    SettingsRowItem(
                        icon = Icons.Outlined.Info,
                        title = "Mother of All Apps",
                        subtitle = "Modular Android Tool System",
                        iconTint = Color(0xFF94A3B8),
                        iconBackgroundColor = Color(0xFF1E222D),
                        onInfoClick = {
                            activeInfoDialog = "About Mother of All Apps" to
                                "MotherOfAllApps · Android 14+ (API 34..36)\n\nArchitecture: Jetpack Compose + Modular Tool Plugins\nDesign Identity: Plain Minimalist Dark UI System\nTelemetry & Diagnostic Hub"
                        },
                        trailingContent = {
                            Text(
                                text = "v0.15.0 (Build 15)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )

                    SettingsDivider()

                    // Row 2: Open Source Libraries & Licenses
                    SettingsRowItem(
                        icon = Icons.Outlined.Code,
                        title = "Open Source Libraries",
                        subtitle = "${dev.pritam.host.settings.model.OpenSourceRegistry.libraries.size} third-party components",
                        iconTint = currentPalette.primary,
                        iconBackgroundColor = currentPalette.primary.copy(alpha = 0.12f),
                        onInfoClick = {
                            activeInfoDialog = "Open Source Libraries" to
                                "Mother of All Apps is built using premier open-source frameworks and libraries licensed under Apache 2.0, EPL, and other permissive licenses."
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingBadge(
                                    text = "${dev.pritam.host.settings.model.OpenSourceRegistry.libraries.size} LIBS",
                                    color = currentPalette.primary
                                )
                                SettingChevron(isExpanded = openSourceExpanded)
                            }
                        },
                        onClick = { openSourceExpanded = !openSourceExpanded }
                    )

                    // Inline Expand: Open Source Libraries List
                    AnimatedVisibility(
                        visible = openSourceExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1016))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dev.pritam.host.settings.model.OpenSourceRegistry.libraries.forEach { lib ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF14161F),
                                    border = BorderStroke(1.dp, Color(0xFF222531)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lib.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color(0xFFF1F5F9),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = currentPalette.primary.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, currentPalette.primary.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = lib.license,
                                                    color = currentPalette.primary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lib.category,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "v${lib.version}",
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Text(
                                            text = lib.description,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Row 3: Reset to Defaults
                    SettingsRowItem(
                        icon = Icons.Outlined.RestartAlt,
                        title = "Reset Settings to Default",
                        subtitle = "Restore all preferences to factory defaults",
                        iconTint = Color(0xFF94A3B8),
                        iconBackgroundColor = Color(0xFF1E222D),
                        trailingContent = {
                            Surface(
                                onClick = {
                                    AppSettingsManager.resetToDefaults()
                                    Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1B1D26),
                                border = BorderStroke(1.dp, Color(0xFF2C303E))
                            ) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    activeInfoDialog?.let { (title, description) ->
        SettingInfoDialog(
            title = title,
            description = description,
            accentColor = currentPalette.primary,
            onDismiss = { activeInfoDialog = null }
        )
    }

    if (showCreatePaletteDialog) {
        CreateCustomPaletteDialog(
            activePrimary = currentPalette.primary,
            onDismiss = { showCreatePaletteDialog = false },
            onCreate = { name, primary, secondary, tertiary ->
                val created = AppSettingsManager.addCustomPalette(name, primary, secondary, tertiary)
                Toast.makeText(context, "Created & applied ${created.displayName}", Toast.LENGTH_SHORT).show()
                showCreatePaletteDialog = false
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// REUSABLE MINIMALIST SETTINGS COMPONENTS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF717686),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(start = 6.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color(0xFF20222A)), RoundedCornerShape(16.dp)),
        color = Color(0xFF14151B),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = Color(0xFF94A3B8),
    iconBackgroundColor: Color = Color(0xFF1C1E26),
    onInfoClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp
                    )
                    if (onInfoClick != null) {
                        IconButton(
                            onClick = onInfoClick,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info for $title",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = Color(0xFF1E2028),
        thickness = 1.dp,
        modifier = modifier.padding(start = 58.dp)
    )
}

@Composable
private fun SettingChevron(
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rot"
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = Color(0xFF4B5263),
        modifier = modifier
            .size(18.dp)
            .rotate(rotation)
    )
}

@Composable
private fun SettingBadge(
    text: String,
    color: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SettingInfoDialog(
    title: String,
    description: String,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Color(0xFF222531)), RoundedCornerShape(14.dp)),
            color = Color(0xFF12141C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.8.sp,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LiquidGlassButton(
                        text = "GOT IT",
                        onClick = onDismiss,
                        glowColor = accentColor,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCustomPaletteDialog(
    activePrimary: Color,
    onDismiss: () -> Unit,
    onCreate: (name: String, primary: Color, secondary: Color, tertiary: Color) -> Unit,
) {
    var themeName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Primary, 1: Secondary, 2: Tertiary

    var primaryColor by remember { mutableStateOf(Color(0xFF38BDF8)) }
    var secondaryColor by remember { mutableStateOf(Color(0xFF818CF8)) }
    var tertiaryColor by remember { mutableStateOf(Color(0xFF34D399)) }

    var hexInput by remember { mutableStateOf(AccentPalette.colorToHex(primaryColor).removePrefix("#")) }

    val swatches = remember {
        listOf(
            Color(0xFF38BDF8),
            Color(0xFF0284C7),
            Color(0xFF6366F1),
            Color(0xFFA855F7),
            Color(0xFFEC4899),
            Color(0xFFF43F5E),
            Color(0xFFFB923C),
            Color(0xFFF59E0B),
            Color(0xFF10B981),
            Color(0xFF34D399),
            Color(0xFFE2E8F0),
            Color(0xFF94A3B8)
        )
    }

    val currentEditingColor = when (selectedTab) {
        0 -> primaryColor
        1 -> secondaryColor
        else -> tertiaryColor
    }

    fun updateActiveColor(color: Color) {
        when (selectedTab) {
            0 -> primaryColor = color
            1 -> secondaryColor = color
            else -> tertiaryColor = color
        }
        hexInput = AccentPalette.colorToHex(color).removePrefix("#")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Color(0xFF222531)), RoundedCornerShape(14.dp)),
            color = Color(0xFF101117)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW ACCENT THEME",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Live Preview Surface
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF14161F),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(primaryColor))
                                Box(Modifier.size(14.dp).clip(CircleShape).background(secondaryColor))
                                Box(Modifier.size(14.dp).clip(CircleShape).background(tertiaryColor))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = themeName.ifBlank { "Live Preview" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = primaryColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = themeName,
                    onValueChange = { if (it.length <= 25) themeName = it },
                    label = { Text("Theme Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Cyber Neon, Stealth...", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = liquidGlassTextFieldColors(focusedBorderColor = primaryColor)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF14161F))
                        .border(1.dp, Color(0xFF222531), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tabs = listOf("Primary" to primaryColor, "Secondary" to secondaryColor, "Tertiary" to tertiaryColor)
                    tabs.forEachIndexed { index, (label, color) ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = {
                                selectedTab = index
                                val target = when (index) {
                                    0 -> primaryColor
                                    1 -> secondaryColor
                                    else -> tertiaryColor
                                }
                                hexInput = AccentPalette.colorToHex(target).removePrefix("#")
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) Color(0xFF1E222F) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "SELECT COLOR PRESET",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(swatches) { swatch ->
                        val isSelected = swatch == currentEditingColor
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color.White else Color(0x33FFFFFF)
                                    ),
                                    CircleShape
                                )
                                .clickable { updateActiveColor(swatch) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = if (swatch.red * 0.299 + swatch.green * 0.587 + swatch.blue * 0.114 > 0.6) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            hexInput = filtered
                            if (filtered.length == 6) {
                                val parsed = AccentPalette.parseHexColor("#$filtered", currentEditingColor)
                                when (selectedTab) {
                                    0 -> primaryColor = parsed
                                    1 -> secondaryColor = parsed
                                    else -> tertiaryColor = parsed
                                }
                            }
                        },
                        prefix = { Text("#", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        label = { Text("Hex Code", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = liquidGlassTextFieldColors(focusedBorderColor = primaryColor)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(currentEditingColor)
                            .border(1.dp, Color(0xFF222531), RoundedCornerShape(6.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassButton(
                        text = "SAVE & APPLY",
                        onClick = {
                            val finalName = themeName.trim().ifBlank { "Custom Theme" }
                            onCreate(finalName, primaryColor, secondaryColor, tertiaryColor)
                        },
                        glowColor = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
