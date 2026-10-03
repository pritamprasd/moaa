package dev.pritam.host.settings.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pritam.ghostagent.GhostAgentManager
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
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
    val currentOpacity by AppSettingsManager.dialogOpacityPercent.collectAsStateWithLifecycle()
    val currentRetention by AppSettingsManager.logRetentionPolicy.collectAsStateWithLifecycle()

    // ── Persistent Section expanded state (Collapsed by default: false) ────
    val generalExpanded    by AppSettingsManager.sectionGeneralExpanded.collectAsStateWithLifecycle()
    val aiExpanded         by AppSettingsManager.sectionAiExpanded.collectAsStateWithLifecycle()
    val automationExpanded by AppSettingsManager.sectionAutomationExpanded.collectAsStateWithLifecycle()
    val diagnosticsExpanded by AppSettingsManager.sectionDiagnosticsExpanded.collectAsStateWithLifecycle()
    val aboutExpanded      by AppSettingsManager.sectionAboutExpanded.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = { GlassBackButton(onClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ══ GENERAL ══════════════════════════════════════════════════════
            settingsSection(
                title = "General",
                emoji = "⚙",
                accentColor = currentPalette.primary,
                isExpanded = generalExpanded,
                onToggle = { AppSettingsManager.setSectionGeneralExpanded(!generalExpanded) }
            ) {
                item {
                    SystemManualSettingsCard(onOpenSystemManual = onOpenSystemManual)
                }
                item {
                    AccentPaletteSettingsCard(
                        currentPalette = currentPalette,
                        customPalettes = customPalettes,
                        onSelectPalette = {
                            AppSettingsManager.setAccentPalette(it)
                            Toast.makeText(context, "Accent theme set to ${it.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onCreateCustomPalette = { name, primary, secondary, tertiary ->
                            val created = AppSettingsManager.addCustomPalette(name, primary, secondary, tertiary)
                            Toast.makeText(context, "Created & applied ${created.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteCustomPalette = { id ->
                            AppSettingsManager.deleteCustomPalette(id)
                            Toast.makeText(context, "Custom palette removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    GalleryLayoutSettingsCard(
                        currentColumns = currentColumns,
                        onSelectColumns = {
                            AppSettingsManager.setGalleryColumnCount(it)
                            Toast.makeText(context, "Gallery layout set to $it column${if (it > 1) "s" else ""}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    DashboardPaddingSettingsCard(
                        currentPadding = currentPadding,
                        onPaddingChange = {
                            AppSettingsManager.setDashboardPadding(it)
                        }
                    )
                }
                item {
                    DialogOpacitySettingsCard(
                        currentOpacity = currentOpacity,
                        onOpacityChange = {
                            AppSettingsManager.setDialogOpacity(it)
                        }
                    )
                }
                item {
                    HomescreenShortcutsCard(context = context)
                }
            }

            // ══ AI & LLM ═════════════════════════════════════════════════════
            settingsSection(
                title = "AI & LLM",
                emoji = "🤖",
                accentColor = Violet,
                isExpanded = aiExpanded,
                onToggle = { AppSettingsManager.setSectionAiExpanded(!aiExpanded) }
            ) {
                item { DefaultLlmSettingsCard(context = context) }
                item { McpServersSettingsCard(context = context) }
                item { LlmGatewaySettingsCard(context = context) }
            }

            // ══ AUTOMATION (GHOST AGENT) ══════════════════════════════════════
            settingsSection(
                title = "Automation",
                emoji = "👻",
                accentColor = Violet,
                isExpanded = automationExpanded,
                onToggle = { AppSettingsManager.setSectionAutomationExpanded(!automationExpanded) }
            ) {
                item { GhostAgentSettingsCard(context = context) }
            }

            // ══ DIAGNOSTICS ═══════════════════════════════════════════════════
            settingsSection(
                title = "Diagnostics",
                emoji = "🔬",
                accentColor = Amber,
                isExpanded = diagnosticsExpanded,
                onToggle = { AppSettingsManager.setSectionDiagnosticsExpanded(!diagnosticsExpanded) }
            ) {
                item {
                    DiagnosticsSettingsCard(
                        currentRetention = currentRetention,
                        onSelectRetention = { policy ->
                            AppSettingsManager.setLogRetentionPolicy(policy)
                            val purged = AppLogHub.pruneExpiredLogs(policy)
                            val msg = if (purged > 0) "Retention: ${policy.displayName} ($purged old logs purged)"
                                      else "Retention: ${policy.displayName}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onOpenLogViewer = onOpenLogViewer,
                        onClearLogs = {
                            AppLogHub.clear()
                            Toast.makeText(context, "Diagnostic logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ══ ABOUT ═════════════════════════════════════════════════════════
            settingsSection(
                title = "About",
                emoji = "ℹ️",
                accentColor = Color(0xFF94A3B8),
                isExpanded = aboutExpanded,
                onToggle = { AppSettingsManager.setSectionAboutExpanded(!aboutExpanded) }
            ) {
                item {
                    AboutInfoCard(
                        onResetDefaults = {
                            AppSettingsManager.resetToDefaults()
                            Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/**
 * Extension on LazyListScope that renders a collapsible section header + animated content block.
 * The header is always visible; the content block slides in/out with spring animation.
 */
private fun LazyListScope.settingsSection(
    title: String,
    emoji: String,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    item(key = "section_header_$title") {
        SettingsSectionHeader(
            title = title,
            emoji = emoji,
            accentColor = accentColor,
            isExpanded = isExpanded,
            onToggle = onToggle,
        )
    }
    if (isExpanded) {
        content()
        item(key = "section_spacer_$title") { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    emoji: String,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chevron_$title"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .background(accentColor.copy(alpha = if (isExpanded) 0.08f else 0.04f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = if (isExpanded) 0.25f else 0.12f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(emoji, fontSize = 16.sp)
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                fontSize = 11.sp
            )
        }

        // Animated chevron: ▼ when expanded, ▶ when collapsed
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = "▼",
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .rotate(chevronAngle),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun SettingHeaderWithInfo(
    title: String,
    description: String,
    accentColor: Color = Cyan,
    badgeText: String? = null,
    badgeColor: Color = accentColor,
    leadingEmoji: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (leadingEmoji != null) {
                Text(leadingEmoji, fontSize = 16.sp)
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                letterSpacing = 1.sp,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = { showDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info for $title",
                    tint = TextSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }

    if (showDialog) {
        SettingInfoDialog(
            title = title,
            description = description,
            accentColor = accentColor,
            onDismiss = { showDialog = false }
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
private fun SystemManualSettingsCard(onOpenSystemManual: () -> Unit) {
    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "System Manual",
                description = "Interactive system manual, architectural specifications, tool guides, and design guidelines for Mother of All Apps.",
                accentColor = Cyan,
                badgeText = "DOCS"
            )
            Spacer(Modifier.height(10.dp))
            LiquidGlassButton(
                onClick = onOpenSystemManual,
                modifier = Modifier.fillMaxWidth(),
                glowColor = Cyan,
                useRainbowBorder = true,
                text = "Open System Manual"
            )
        }
    }
}

@Composable
private fun AccentPaletteSettingsCard(
    currentPalette: dev.pritam.host.settings.AccentPalette,
    customPalettes: List<dev.pritam.host.settings.AccentPalette>,
    onSelectPalette: (dev.pritam.host.settings.AccentPalette) -> Unit,
    onCreateCustomPalette: (name: String, primary: Color, secondary: Color, tertiary: Color) -> Unit,
    onDeleteCustomPalette: (String) -> Unit,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    IsometricCard(glowColor = currentPalette.primary) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Accent Color Theme",
                description = "Choose from 5 curated minimalist dark palettes tuned for high contrast and minimal glare, or tap (+) to create custom tri-color combinations with instant system-wide application.",
                accentColor = currentPalette.primary,
                badgeText = if (currentPalette.isCustom) "CUSTOM" else currentPalette.displayName
            )

            Spacer(Modifier.height(10.dp))

            // ── Single Row: [ Dropdown Menu Trigger (weight 1f) ] [ + New Accent Button ] ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown trigger
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { isDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF14161F),
                        border = BorderStroke(1.dp, Color(0xFF222531)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(currentPalette.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(currentPalette.secondary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(currentPalette.tertiary)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = currentPalette.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Accent Theme Dropdown",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(if (isDropdownExpanded) 180f else 0f)
                            )
                        }
                    }

                    // Dropdown menu options
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
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

                        dev.pritam.host.settings.AccentPalette.builtInPalettes.forEach { palette ->
                            val isSelected = palette.id == currentPalette.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.primary)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.secondary)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.tertiary)
                                            )
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
                                    onSelectPalette(palette)
                                    isDropdownExpanded = false
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
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.primary)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.secondary)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.tertiary)
                                                )
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
                                                Text(
                                                    text = "✓",
                                                    color = palette.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteCustomPalette(palette.id) },
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
                                        onSelectPalette(palette)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // (+) New Accent Button in the same row
                Surface(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF14161F),
                    border = BorderStroke(1.dp, currentPalette.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Custom Accent",
                            tint = currentPalette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomPaletteDialog(
            activePrimary = currentPalette.primary,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, primary, secondary, tertiary ->
                onCreateCustomPalette(name, primary, secondary, tertiary)
                showCreateDialog = false
            }
        )
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

    var hexInput by remember { mutableStateOf(dev.pritam.host.settings.AccentPalette.colorToHex(primaryColor).removePrefix("#")) }

    // Quick swatches
    val swatches = remember {
        listOf(
            Color(0xFF38BDF8), // Cyan
            Color(0xFF0284C7), // Sky Blue
            Color(0xFF6366F1), // Indigo
            Color(0xFFA855F7), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFFF43F5E), // Rose
            Color(0xFFFB923C), // Orange
            Color(0xFFF59E0B), // Amber
            Color(0xFF10B981), // Emerald
            Color(0xFF34D399), // Mint
            Color(0xFFE2E8F0), // Platinum
            Color(0xFF94A3B8)  // Slate
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
        hexInput = dev.pritam.host.settings.AccentPalette.colorToHex(color).removePrefix("#")
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
                // Header
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
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(secondaryColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(tertiaryColor)
                                )
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

                // Name input
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

                // Segmented Tab for Primary / Secondary / Tertiary
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
                                hexInput = dev.pritam.host.settings.AccentPalette.colorToHex(target).removePrefix("#")
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
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
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

                // Color swatches row
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

                // Hex input
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
                                val parsed = dev.pritam.host.settings.AccentPalette.parseHexColor("#$filtered", currentEditingColor)
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

                // Actions: CANCEL / SAVE & APPLY
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

@Composable
private fun GalleryLayoutSettingsCard(
    currentColumns: Int,
    onSelectColumns: (Int) -> Unit,
) {
    val layoutDescription = when (currentColumns) {
        1 -> "1 Column: Full-width detailed list with expanded descriptions"
        2 -> "2 Columns: Dual isometric cards (Balanced Default)"
        3 -> "3 Columns: Matrix grid for high density & rapid launch"
        4 -> "4 Columns: Micro-deck dense tiles for compact overview"
        else -> "$currentColumns Columns Grid"
    }

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Home Gallery Layout",
                description = "Configure how tools are arranged on the home dashboard:\n\n• 1 Column: Full-width detailed list with expanded descriptions\n• 2 Columns: Dual isometric cards (Balanced Default)\n• 3 Columns: Matrix grid for high density & rapid launch\n• 4 Columns: Micro-deck dense tiles for compact overview",
                accentColor = Cyan,
                badgeText = "$currentColumns Column${if (currentColumns > 1) "s" else ""}"
            )

            Spacer(Modifier.height(10.dp))

            // Column count slider (1 to 4 columns)
            Slider(
                value = currentColumns.toFloat(),
                onValueChange = { onSelectColumns(it.toInt().coerceIn(1, 4)) },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan,
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Quick Preset Column Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..4).forEach { cols ->
                    val isSel = currentColumns == cols
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                        border = BorderStroke(1.dp, if (isSel) Cyan else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectColumns(cols) }
                    ) {
                        Text(
                            text = if (cols == 1) "1 Col" else "$cols Cols",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) Cyan else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPaddingSettingsCard(
    currentPadding: Int,
    onPaddingChange: (Int) -> Unit,
) {
    val presetDescription = when {
        currentPadding <= 10 -> "High Density / Compact"
        currentPadding <= 16 -> "Balanced"
        currentPadding <= 22 -> "Comfortable (Default)"
        else -> "Spacious / Relaxed"
    }

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Dashboard Padding & Spacing",
                description = "Adjust outer screen margins and card spacing across the dashboard ($presetDescription). Range: 8dp to 32dp.",
                accentColor = Cyan,
                badgeText = "$currentPadding dp"
            )

            Spacer(Modifier.height(10.dp))

            // Padding Slider (8dp to 32dp in steps of 2dp)
            Slider(
                value = currentPadding.toFloat(),
                onValueChange = { onPaddingChange(it.toInt()) },
                valueRange = 8f..32f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan,
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Preset Quick Buttons Row
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
                        color = if (isSel) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                        border = BorderStroke(1.dp, if (isSel) Cyan else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPaddingChange(dpValue) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) Cyan else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogOpacitySettingsCard(
    currentOpacity: Int,
    onOpacityChange: (Int) -> Unit,
) {
    val opacityDescription = when {
        currentOpacity <= 60 -> "High Transparency (Translucent)"
        currentOpacity <= 80 -> "Medium Frost (Aero Glass)"
        currentOpacity <= 95 -> "High Contrast Acrylic (Recommended)"
        else -> "100% Solid Opaque"
    }

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Dialog & Popup Opacity",
                description = "Controls background bleed-through for dialogs and popups ($opacityDescription). Range: 50% to 100% opacity.",
                accentColor = Cyan,
                badgeText = "$currentOpacity%"
            )

            Spacer(Modifier.height(10.dp))

            // Opacity Slider (50% to 100% in steps of 5%)
            Slider(
                value = currentOpacity.toFloat(),
                onValueChange = { onOpacityChange(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan,
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Preset Quick Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    70 to "Frost (70%)",
                    94 to "Focus (94%)",
                    100 to "Solid (100%)"
                ).forEach { (opacityVal, label) ->
                    val isSel = currentOpacity == opacityVal
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                        border = BorderStroke(1.dp, if (isSel) Cyan else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpacityChange(opacityVal) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) Cyan else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Live Dialog Surface Preview Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppSettingsManager.getDialogSurfaceColor(currentOpacity),
                border = BorderStroke(1.dp, RainbowGlassBorderBrush),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PREVIEW: Dialog Content Clarity",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Background visibility is dimmed according to this setting",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Cyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Cyan)
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsSettingsCard(
    currentRetention: dev.pritam.host.settings.LogRetentionPolicy,
    onSelectRetention: (dev.pritam.host.settings.LogRetentionPolicy) -> Unit,
    onOpenLogViewer: () -> Unit,
    onClearLogs: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    IsometricCard(glowColor = Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "System Diagnostics & Logging",
                description = "Manage real-time telemetry, auto-delete policy, and memory cleanup. View live diagnostic streams or purge logs to reclaim storage.",
                accentColor = Rose,
                badgeText = currentRetention.displayName.uppercase()
            )

            Spacer(Modifier.height(10.dp))

            // Auto-delete / retention policy dropdown section
            Text(
                text = "AUTO-DELETE OLD LOGS (RETENTION POLICY)",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))

            // Dropdown anchor box
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlassSurfaceDeep,
                    border = BorderStroke(1.dp, if (dropdownExpanded) Rose else GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dropdownExpanded = !dropdownExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⏱️", fontSize = 16.sp)
                            Column {
                                Text(
                                    text = currentRetention.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Rose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = currentRetention.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = if (dropdownExpanded) "▲" else "▼",
                            color = Rose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .background(dev.pritam.host.settings.AppSettingsManager.getDialogSurfaceColor())
                        .border(BorderStroke(1.dp, Rose.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                ) {
                    dev.pritam.host.settings.LogRetentionPolicy.entries.forEach { policy ->
                        val isSelected = currentRetention == policy
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = policy.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Rose else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = policy.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            color = Rose,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                onSelectRetention(policy)
                            }
                        )
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
    val tools = dev.pritam.host.config.ToolRegistryConfig.INSTALLED_TOOLS

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Homescreen Shortcuts",
                description = "Tap any tool icon below to pin a 1-tap direct launch shortcut to your Android launcher home screen.",
                accentColor = Cyan,
                badgeText = "PIN TO LAUNCHER"
            )

            Spacer(Modifier.height(10.dp))

            val chunkedTools = tools.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunkedTools.forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTools.forEach { tool ->
                            val accentColor = Color(tool.accentColorHex)
                            val shape = RoundedCornerShape(10.dp)
                            val emoji = when (tool.iconType) {
                                "ftp" -> "📡"
                                "nfc" -> "📶"
                                "ftp-client" -> "☁️"
                                "logs" -> "📋"
                                "sensors" -> "🧭"
                                "brain" -> "🧠"
                                "chat" -> "💬"
                                "dynamic-tool" -> "⚡"
                                "manual" -> "📖"
                                "ghost" -> "👻"
                                "terminal" -> "💻"
                                "system-info" -> "ℹ️"
                                else -> "🔧"
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shape)
                                    .clickable {
                                        dev.pritam.host.shortcut.ShortcutUtils.pinToolToHomeScreen(context, tool)
                                    }
                                    .border(
                                        BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                        shape
                                    ),
                                color = GlassSurfaceDeep,
                                shape = shape
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        modifier = Modifier.size(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = emoji,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = tool.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }

                        // Pad out remaining columns if last row has less than 3 tools
                        repeat(3 - rowTools.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LlmGatewaySettingsCard(context: Context) {
    val httpServer = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getHttpServer(context)
    val telemetry by httpServer.telemetry.collectAsStateWithLifecycle()

    IsometricCard(glowColor = if (telemetry.isRunning) Color(0xFF34D399) else Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Local LLM Gateway Server",
                description = "Embedded local HTTP loopback server (http://127.0.0.1:8080) for container tools and local apps with OpenAI-compatible API spec, multi-account pooling & failover.",
                accentColor = if (telemetry.isRunning) Color(0xFF34D399) else Rose,
                badgeText = if (telemetry.isRunning) "● RUNNING :8080" else "○ STOPPED",
                badgeColor = if (telemetry.isRunning) Color(0xFF34D399) else Rose
            )

            Spacer(Modifier.height(10.dp))

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
            SettingHeaderWithInfo(
                title = "About Mother of All Apps",
                description = "MotherOfAllApps · Android 14+ (API 34..36)\n\nArchitecture: Jetpack Compose + Modular Tool Plugins\nDesign Identity: Plain Minimalist Dark UI System\nTelemetry & Diagnostic Hub",
                accentColor = Color(0xFF94A3B8),
                badgeText = "v0.15.0"
            )

            Spacer(Modifier.height(10.dp))

            LiquidGlassButton(
                onClick = onResetDefaults,
                modifier = Modifier.fillMaxWidth(),
                glowColor = Color(0xFF94A3B8),
                text = "Reset App Preferences to Defaults"
            )
        }
    }
}

@Composable
private fun DefaultLlmSettingsCard(context: Context) {
    val repository = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getRepository(context)
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val defaultProfile = profiles.firstOrNull { it.isEnabled } ?: profiles.firstOrNull()

    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "Default LLM Route",
                description = "Select the primary model used for CyberChat and default requests. If offline or rate-limited, requests fail over to the next priority target in the profile pool.",
                accentColor = Cyan,
                badgeText = "PRIORITY #1"
            )

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { profile ->
                    val isDefault = profile.id == defaultProfile?.id
                    val borderColor = if (isDefault) Cyan else GlassBorder
                    val surfaceColor = if (isDefault) Cyan.copy(alpha = 0.12f) else GlassSurfaceDeep

                    Surface(
                        shape = RoundedCornerShape(10.dp),
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Radio selection indicator
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = 2.dp,
                                            color = if (isDefault) Cyan else TextSecondary.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDefault) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Cyan, CircleShape)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDefault) Cyan else TextPrimary,
                                            fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        if (isDefault) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Cyan.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    color = Cyan,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${profile.providerType} • ${profile.targetModel ?: "auto"} • ${if (profile.isEnabled) "Enabled" else "Disabled"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Status Indicator
                            val statusColor = when (profile.status) {
                                dev.pritam.host.tool.llmgateway.model.ProfileStatus.ACTIVE -> Emerald
                                dev.pritam.host.tool.llmgateway.model.ProfileStatus.IDLE -> TextSecondary
                                dev.pritam.host.tool.llmgateway.model.ProfileStatus.RATE_LIMITED -> Amber
                                dev.pritam.host.tool.llmgateway.model.ProfileStatus.HOST_UNREACHABLE -> Rose
                                dev.pritam.host.tool.llmgateway.model.ProfileStatus.EXPIRED -> Rose
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = profile.status.name,
                                    color = statusColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
private fun McpServersSettingsCard(context: Context) {
    val mcpRepository = dev.pritam.host.tool.llmgateway.manager.LlmGatewayManager.getMcpRepository(context)
    val servers by mcpRepository.servers.collectAsStateWithLifecycle()
    val activeToolsCount = servers.filter { it.isEnabled }.sumOf { s -> s.discoveredTools.count { it.isEnabled } }

    IsometricCard(glowColor = Violet) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingHeaderWithInfo(
                title = "MCP Tool Servers",
                description = "Turn Model Context Protocol servers on or off. Active tools (hardware sensors, FTP, system logs, remote APIs) are dynamically exposed to LLMs for automated tool calling.",
                accentColor = Violet,
                badgeText = "$activeToolsCount ACTIVE TOOLS"
            )

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                servers.forEach { server ->
                    val isBuiltin = server.transportType == dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType.BUILTIN_DEVICE
                    val enabledTools = server.discoveredTools.count { it.isEnabled }
                    val totalTools = server.discoveredTools.size

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, if (server.isEnabled) Violet.copy(alpha = 0.5f) else GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        color = if (server.isEnabled) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
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
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isBuiltin) "$enabledTools of $totalTools device tools active" else "${server.endpointUrl.ifBlank { "No endpoint" }} • $enabledTools tools",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Switch(
                                checked = server.isEnabled,
                                onCheckedChange = { isChecked ->
                                    mcpRepository.toggleServerEnabled(server.id, isChecked)
                                    Toast.makeText(
                                        context,
                                        "MCP Server '${server.name}' ${if (isChecked) "Enabled" else "Disabled"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Violet,
                                    checkedTrackColor = Violet.copy(alpha = 0.35f),
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = GlassSurfaceElevated
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostAgentSettingsCard(context: android.content.Context) {
    val isConnected = GhostAgentManager.isAccessibilityServiceConnected()
    val isBubbleVisible = GhostAgentManager.isBubbleVisible()
    val statusColor = if (isConnected) Color(0xFF34D399) else Amber

    IsometricCard(glowColor = Violet) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title row
            SettingHeaderWithInfo(
                title = "Ghost Agent",
                description = "Automates multi-step tasks across any app using Android Accessibility Service. Enable A11y access in system settings to start automation workflows.",
                accentColor = Violet,
                badgeText = if (isConnected) "● A11Y ON" else "○ A11Y OFF",
                badgeColor = statusColor,
                leadingEmoji = "👻"
            )

            // Floating Bubble toggle row (Switch)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, if (isBubbleVisible) Violet.copy(alpha = 0.4f) else GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Floating Bubble",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Always-on 👻 overlay over other apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
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
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = GlassSurfaceElevated
                        )
                    )
                }
            }

            // Open Accessibility Settings button
            LiquidGlassButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                glowColor = if (isConnected) Color(0xFF34D399) else Amber,
                text = if (isConnected) "✓ Accessibility Service Active" else "⚠ Enable Accessibility Service →"
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun SettingsScreenPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        SettingsScreen(
            onNavigateBack = {},
            onOpenLogViewer = {},
            onOpenSystemManual = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun DialogOpacitySettingsCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        DialogOpacitySettingsCard(
            currentOpacity = 94,
            onOpacityChange = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun DashboardPaddingSettingsCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        DashboardPaddingSettingsCard(
            currentPadding = 18,
            onPaddingChange = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun GalleryLayoutSettingsCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        GalleryLayoutSettingsCard(
            currentColumns = 2,
            onSelectColumns = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun DiagnosticsSettingsCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        DiagnosticsSettingsCard(
            currentRetention = dev.pritam.host.settings.LogRetentionPolicy.ONE_DAY,
            onSelectRetention = {},
            onOpenLogViewer = {},
            onClearLogs = {}
        )
    }
}


