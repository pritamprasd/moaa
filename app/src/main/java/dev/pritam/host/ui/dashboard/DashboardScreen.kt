package dev.pritam.host.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pritam.host.config.ToolRegistryConfig
import dev.pritam.host.settings.AppSettingsManager
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SpaceBackground
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.SurfaceElevated
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet
import dev.pritam.pluginapi.ToolInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    tools: List<ToolInfo>,
    modifier: Modifier = Modifier,
    onToolClick: (ToolInfo) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val columnCount by AppSettingsManager.galleryColumnCount.collectAsStateWithLifecycle()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) {
            tools
        } else {
            tools.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.id.value.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "MOTHER OF ALL APPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.5.sp
                    )
                },
                actions = {
                    // Search Expand Icon Button with active glow toggle
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSearchExpanded) Cyan.copy(alpha = 0.2f) else Color(0x221E293B))
                                .border(BorderStroke(1.dp, if (isSearchExpanded) Cyan else dev.pritam.host.ui.theme.GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IsometricSearchIcon(color = if (isSearchExpanded) Cyan else TextPrimary)
                        }
                    }

                    // Settings Icon Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, dev.pritam.host.ui.theme.GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IsometricSettingsIcon(color = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            // Smooth Animated Search Bar (Appears directly below Header)
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search tools by name, tag, or description...", fontSize = 12.sp, color = TextSecondary) },
                        singleLine = true,
                        leadingIcon = {
                            IsometricSearchIcon(color = Cyan)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Clear", color = Rose, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = dev.pritam.host.ui.theme.GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = dev.pritam.host.ui.theme.GlassSurfaceDeep,
                            unfocusedContainerColor = dev.pritam.host.ui.theme.GlassSurfaceDeep
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(BorderStroke(1.dp, dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush), RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Tool Gallery Grid (Columns dynamically bound to AppSettingsManager)
            if (filteredTools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tools found matching '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("Clear Search Filter", color = Cyan)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {                    items(filteredTools, key = { it.id.value }) { tool ->
                        ExpandableToolGalleryCard(
                            tool = tool,
                            columnCount = columnCount,
                            onLaunch = { onToolClick(tool) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Isometric Tool Card with expandable metadata drawer.
 * No hardcoded version or "open" labels. Clicking chevron reveals full description,
 * version, category, author, and required permissions.
 */
@Composable
private fun ExpandableToolGalleryCard(
    tool: ToolInfo,
    columnCount: Int,
    onLaunch: () -> Unit,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val definition = remember(tool.id.value) {
        ToolRegistryConfig.findToolDefinition(tool.id.value)
    }

    val accentColor = when {
        tool.id.value == "ftp-server" -> Cyan
        tool.id.value == "ftp-client" -> Color(0xFF34D399)
        tool.id.value == "nfc-tool" -> Rose
        tool.id.value == "log-viewer" -> Violet
        tool.id.value == "sensors" -> Color(0xFFF59E0B)
        tool.id.value == "llm-gateway" -> Color(0xFF10B981)
        tool.id.value == "llm-chat" -> Color(0xFF38BDF8)
        tool.id.value == "dynamic-tools-studio" -> Color(0xFFF43F5E) // Rose-Red
        tool.id.value.startsWith("dynamic_") -> Color(0xFF38BDF8) // Dynamic Web Tool
        else -> Cyan
    }

    val iconType = definition?.iconType ?: if (tool.id.value.startsWith("dynamic_")) "dynamic-tool" else "generic"
    val shape = RoundedCornerShape(14.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Clean Liquid Glass Depth Shadow (No duplicate border strokes!)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 3.dp)
                .clip(shape)
                .background(Color(0x50030712))
        )

        // 2. Main Frosted Acrylic Glass Layer with Single Radiant Rainbow Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isExpanded) accentColor.copy(alpha = 0.18f) else Color(0x3E1E293B),
                            Color(0x1D0F172A)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush),
                    shape
                )
        ) {
            // Specular Top Shine Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color(0xFF38BDF8).copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLaunch() }
                    .padding(if (columnCount >= 3) 8.dp else 14.dp)
            ) {
                // Top Row: Icon + Name + Modern Animated Chevron Expander
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ToolIsometricIcon(
                            iconType = iconType,
                            color = accentColor,
                            size = if (columnCount == 1) 42.dp else if (columnCount == 2) 34.dp else 26.dp
                        )

                        Spacer(Modifier.width(8.dp))

                        Column {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = if (columnCount >= 3) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = if (columnCount == 1) 14.sp else if (columnCount == 2) 12.sp else 10.sp
                            )
                            if (tool.id.value.startsWith("dynamic_") && columnCount <= 2) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0x3038BDF8),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "AI WEB APP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Cyan,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Modern Smooth Chevron Expander
                    TileExpandChevron(
                        isExpanded = isExpanded,
                        accentColor = accentColor,
                        onClick = { isExpanded = !isExpanded }
                    )
                }

                // Short tagline (hidden in 4-column compact mode for cleanliness)
                if (columnCount <= 3 && !isExpanded) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = definition?.shortTagline ?: tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = if (columnCount == 1) 11.sp else 10.sp,
                        maxLines = if (columnCount <= 2) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                // Expanded Metadata Drawer
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = dev.pritam.host.ui.theme.GlassSurfaceDeep,
                            border = BorderStroke(1.dp, dev.pritam.host.ui.theme.GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = definition?.description ?: tool.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Version: ${tool.version}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = definition?.category ?: "Tool Module",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }

                                if (!definition?.requiredPermissions.isNullOrEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Permissions: ${definition.requiredPermissions.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                dev.pritam.host.ftp.ui.components.LiquidGlassButton(
                                    onClick = {
                                        definition?.let {
                                            dev.pritam.host.shortcut.ShortcutUtils.pinToolToHomeScreen(context, it)
                                        }
                                    },
                                    text = "📌 Pin to Home Screen",
                                    glowColor = accentColor,
                                    useRainbowBorder = true,
                                    modifier = Modifier.fillMaxWidth().height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D Isometric Search Icon
 */
@Composable
private fun IsometricSearchIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val cx = size.width * 0.4f
        val cy = size.height * 0.4f
        val r = size.width * 0.3f

        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawLine(
            color = color,
            start = Offset(cx + r * 0.7f, cy + r * 0.7f),
            end = Offset(size.width * 0.9f, size.height * 0.9f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Settings Gear Icon
 */
@Composable
private fun IsometricSettingsIcon(color: Color) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = color,
        modifier = Modifier.size(18.dp)
    )
}


/**
 * Custom Canvas rendering vivid Isometric 3D Icons for each tool type.
 */
@Composable
private fun ToolIsometricIcon(
    iconType: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(SurfaceDeep)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(size * 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            when (iconType) {
                "ftp" -> {
                    // Isometric Server Rack with Wi-Fi beam
                    drawRect(
                        color = color.copy(alpha = 0.8f),
                        topLeft = Offset(cx - w * 0.35f, cy - h * 0.3f),
                        size = Size(w * 0.7f, h * 0.6f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = color,
                        start = Offset(cx - w * 0.25f, cy),
                        end = Offset(cx + w * 0.25f, cy),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(cx + w * 0.2f, cy - h * 0.15f))
                }
                "ftp-client" -> {
                    // Isometric Cloud & Remote Stream Arrow
                    val path = Path().apply {
                        moveTo(cx, cy + h * 0.3f)
                        lineTo(cx - w * 0.22f, cy + h * 0.05f)
                        lineTo(cx - w * 0.08f, cy + h * 0.05f)
                        lineTo(cx - w * 0.08f, cy - h * 0.25f)
                        lineTo(cx + w * 0.08f, cy - h * 0.25f)
                        lineTo(cx + w * 0.08f, cy + h * 0.05f)
                        lineTo(cx + w * 0.22f, cy + h * 0.05f)
                        close()
                    }
                    drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                    drawLine(
                        color = color.copy(alpha = 0.6f),
                        start = Offset(cx - w * 0.32f, cy + h * 0.38f),
                        end = Offset(cx + w * 0.32f, cy + h * 0.38f),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                "nfc" -> {
                    // Isometric Concentric NFC Loops
                    drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(color = color.copy(alpha = 0.6f), radius = w * 0.25f, style = Stroke(width = 1.2.dp.toPx()))
                    drawCircle(color = color, radius = 2.dp.toPx())
                }
                "logs" -> {
                    // Terminal Prompt with Heartbeat pulse
                    val path = Path().apply {
                        moveTo(cx - w * 0.35f, cy - h * 0.2f)
                        lineTo(cx - w * 0.1f, cy)
                        lineTo(cx - w * 0.35f, cy + h * 0.2f)
                    }
                    drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                    drawLine(
                        color = color,
                        start = Offset(cx + w * 0.05f, cy + h * 0.2f),
                        end = Offset(cx + w * 0.35f, cy + h * 0.2f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                "sensors" -> {
                    // 3D Gyroscope / Sensor Orbit Rings & Core Node
                    drawCircle(color = color.copy(alpha = 0.8f), radius = w * 0.38f, style = Stroke(width = 1.4.dp.toPx()))
                    drawOval(
                        color = color.copy(alpha = 0.6f),
                        topLeft = Offset(cx - w * 0.35f, cy - h * 0.16f),
                        size = Size(w * 0.7f, h * 0.32f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                    drawLine(color = color.copy(alpha = 0.5f), start = Offset(cx, cy - h * 0.38f), end = Offset(cx, cy + h * 0.38f), strokeWidth = 1.dp.toPx())
                }
                "brain" -> {
                    // Neural AI Node & Synapse Matrix
                    val topNode = Offset(cx, cy - h * 0.32f)
                    val leftNode = Offset(cx - w * 0.3f, cy + h * 0.15f)
                    val rightNode = Offset(cx + w * 0.3f, cy + h * 0.15f)
                    val centerNode = Offset(cx, cy)

                    drawLine(color = color.copy(alpha = 0.5f), start = topNode, end = centerNode, strokeWidth = 1.5.dp.toPx())
                    drawLine(color = color.copy(alpha = 0.5f), start = leftNode, end = centerNode, strokeWidth = 1.5.dp.toPx())
                    drawLine(color = color.copy(alpha = 0.5f), start = rightNode, end = centerNode, strokeWidth = 1.5.dp.toPx())
                    drawLine(color = color.copy(alpha = 0.4f), start = leftNode, end = rightNode, strokeWidth = 1.2.dp.toPx())

                    drawCircle(color = color, radius = 2.8.dp.toPx(), center = topNode)
                    drawCircle(color = color, radius = 2.8.dp.toPx(), center = leftNode)
                    drawCircle(color = color, radius = 2.8.dp.toPx(), center = rightNode)
                    drawCircle(color = color, radius = 3.8.dp.toPx(), center = centerNode)
                }
                "chat" -> {
                    // Isometric Speech Bubble with glowing communication nodes
                    val bubblePath = Path().apply {
                        moveTo(cx - w * 0.35f, cy - h * 0.28f)
                        lineTo(cx + w * 0.35f, cy - h * 0.28f)
                        lineTo(cx + w * 0.35f, cy + h * 0.12f)
                        lineTo(cx - w * 0.05f, cy + h * 0.12f)
                        lineTo(cx - w * 0.25f, cy + h * 0.32f)
                        lineTo(cx - w * 0.2f, cy + h * 0.12f)
                        lineTo(cx - w * 0.35f, cy + h * 0.12f)
                        close()
                    }
                    drawPath(bubblePath, color = color.copy(alpha = 0.85f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx - w * 0.16f, cy - h * 0.08f))
                    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx, cy - h * 0.08f))
                    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx + w * 0.16f, cy - h * 0.08f))
                }
                "dynamic-tool" -> {
                    // Isometric Web Code Window with Prompt Sparkle
                    drawRoundRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(cx - w * 0.35f, cy - h * 0.3f),
                        size = Size(w * 0.7f, h * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Top header line
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(cx - w * 0.35f, cy - h * 0.12f),
                        end = Offset(cx + w * 0.35f, cy - h * 0.12f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Window dots
                    drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(cx - w * 0.24f, cy - h * 0.21f))
                    drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(cx - w * 0.14f, cy - h * 0.21f))
                    // Code brackets </>
                    val leftBracket = Path().apply {
                        moveTo(cx - w * 0.08f, cy + h * 0.02f)
                        lineTo(cx - w * 0.2f, cy + h * 0.14f)
                        lineTo(cx - w * 0.08f, cy + h * 0.26f)
                    }
                    val rightBracket = Path().apply {
                        moveTo(cx + w * 0.08f, cy + h * 0.02f)
                        lineTo(cx + w * 0.2f, cy + h * 0.14f)
                        lineTo(cx + w * 0.08f, cy + h * 0.26f)
                    }
                    drawPath(leftBracket, color = color, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(rightBracket, color = color, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
                }
                "manual" -> {
                    // Isometric Open Manual / Book with Spine and Lines
                    val leftPage = Path().apply {
                        moveTo(cx, cy + h * 0.28f)
                        lineTo(cx - w * 0.35f, cy + h * 0.16f)
                        lineTo(cx - w * 0.35f, cy - h * 0.24f)
                        lineTo(cx, cy - h * 0.12f)
                        close()
                    }
                    val rightPage = Path().apply {
                        moveTo(cx, cy + h * 0.28f)
                        lineTo(cx + w * 0.35f, cy + h * 0.16f)
                        lineTo(cx + w * 0.35f, cy - h * 0.24f)
                        lineTo(cx, cy - h * 0.12f)
                        close()
                    }
                    drawPath(leftPage, color = color.copy(alpha = 0.85f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(rightPage, color = color.copy(alpha = 0.85f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
                    // Central Spine
                    drawLine(color = color, start = Offset(cx, cy - h * 0.12f), end = Offset(cx, cy + h * 0.28f), strokeWidth = 1.5.dp.toPx())
                    // Left Text lines
                    drawLine(color = color.copy(alpha = 0.5f), start = Offset(cx - w * 0.08f, cy - h * 0.04f), end = Offset(cx - w * 0.26f, cy + h * 0.03f), strokeWidth = 1.dp.toPx())
                    drawLine(color = color.copy(alpha = 0.5f), start = Offset(cx - w * 0.08f, cy + h * 0.08f), end = Offset(cx - w * 0.26f, cy + h * 0.15f), strokeWidth = 1.dp.toPx())
                    // Right Text lines
                    drawLine(color = color.copy(alpha = 0.5f), start = Offset(cx + w * 0.08f, cy - h * 0.04f), end = Offset(cx + w * 0.26f, cy + h * 0.03f), strokeWidth = 1.dp.toPx())
                    drawLine(color = color.copy(alpha = 0.5f), start = Offset(cx + w * 0.08f, cy + h * 0.08f), end = Offset(cx + w * 0.26f, cy + h * 0.15f), strokeWidth = 1.dp.toPx())
                }
                else -> {
                    // Generic Cyber Chip
                    drawRect(color = color, topLeft = Offset(cx - w * 0.3f, cy - h * 0.3f), size = Size(w * 0.6f, h * 0.6f), style = Stroke(1.5.dp.toPx()))
                    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(cx, cy))
                }
            }
        }
    }
}

/**
 * Modern animated chevron expander icon for tool cards.
 * Smoothly rotates 180 degrees with spring physics and glows when active.
 */
@Composable
private fun TileExpandChevron(
    isExpanded: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "chevronRotation"
    )

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (isExpanded) accentColor.copy(alpha = 0.2f) else SurfaceDeep)
            .border(
                BorderStroke(1.dp, if (isExpanded) accentColor else Color(0xFF334155)),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer(rotationZ = rotation)
        ) {
            val strokeWidth = 1.8.dp.toPx()
            val w = size.width
            val h = size.height

            val path = Path().apply {
                moveTo(w * 0.18f, h * 0.38f)
                lineTo(w * 0.5f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.38f)
            }

            drawPath(
                path = path,
                color = if (isExpanded) accentColor else TextSecondary,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}