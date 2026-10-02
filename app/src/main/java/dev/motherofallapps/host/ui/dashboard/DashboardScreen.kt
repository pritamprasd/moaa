package dev.motherofallapps.host.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.motherofallapps.host.R
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet
import dev.motherofallapps.pluginapi.ToolId
import dev.motherofallapps.pluginapi.ToolInfo
import dev.motherofallapps.pluginapi.ToolState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    tools: List<ToolInfo>,
    modifier: Modifier = Modifier,
    onToolClick: (ToolInfo) -> Unit = {},
) {
    var columnCount by remember { mutableIntStateOf(2) } // 1, 2, 3, or 4 columns
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
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MOTHER OF ALL APPS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${tools.size} TOOLS READY · MODULAR SUITE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Gallery Controls Header: Column Selector & Search
            GalleryControlsHeader(
                currentColumns = columnCount,
                onSelectColumns = { columnCount = it },
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it }
            )

            Spacer(Modifier.height(14.dp))

            // 2. Dynamic Responsive Grid View
            if (filteredTools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tools match '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredTools, key = { it.id.value }) { tool ->
                        DynamicGalleryToolCard(
                            tool = tool,
                            columnCount = columnCount,
                            onClick = { onToolClick(tool) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryControlsHeader(
    currentColumns: Int,
    onSelectColumns: (Int) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOOL GALLERY LAYOUT",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            // Column Switcher Buttons: 1 Col, 2 Col, 3 Col, 4 Col
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ColumnButton(label = "1 Col", count = 1, current = currentColumns, onSelect = onSelectColumns)
                ColumnButton(label = "2 Col", count = 2, current = currentColumns, onSelect = onSelectColumns)
                ColumnButton(label = "3 Col", count = 3, current = currentColumns, onSelect = onSelectColumns)
                ColumnButton(label = "4 Col", count = 4, current = currentColumns, onSelect = onSelectColumns)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search tools by name or purpose...", fontSize = 12.sp) },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(onClick = { onSearchChange("") }) {
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
    }
}

@Composable
private fun ColumnButton(
    label: String,
    count: Int,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    val isSelected = count == current
    val shape = RoundedCornerShape(6.dp)

    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable { onSelect(count) }
            .border(
                BorderStroke(1.dp, if (isSelected) Cyan else Color(0xFF334155)),
                shape
            ),
        color = if (isSelected) Cyan.copy(alpha = 0.2f) else SurfaceDeep,
        shape = shape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Cyan else TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Responsive Isometric Tool Card adjusting dynamically across 1, 2, 3, and 4 column layouts.
 */
@Composable
private fun DynamicGalleryToolCard(
    tool: ToolInfo,
    columnCount: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)

    val (accentColor, iconType) = when (tool.id.value) {
        "ftp-server" -> Cyan to "ftp"
        "nfc-tool" -> Rose to "nfc"
        "log-viewer" -> Violet to "logs"
        else -> Cyan to "generic"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Isometric 3D Drop Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(shape)
                .background(Color(0xFF070A10))
        )

        // Main Card Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.6f),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                    shape
                ),
            color = SurfaceElevated.copy(alpha = 0.95f),
            shape = shape
        ) {
            when (columnCount) {
                1 -> OneColumnLayout(tool, accentColor, iconType)
                2 -> TwoColumnLayout(tool, accentColor, iconType)
                3 -> ThreeColumnLayout(tool, accentColor, iconType)
                4 -> FourColumnLayout(tool, accentColor, iconType)
            }
        }
    }
}

@Composable
private fun OneColumnLayout(tool: ToolInfo, accentColor: Color, iconType: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolIsometricIcon(iconType = iconType, color = accentColor, size = 52.dp)

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "v${tool.version} · Installed · Tap to launch",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TwoColumnLayout(tool: ToolInfo, accentColor: Color, iconType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            ToolIsometricIcon(iconType = iconType, color = accentColor, size = 42.dp)

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 3,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "v${tool.version} · Open →",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun ThreeColumnLayout(tool: ToolInfo, accentColor: Color, iconType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ToolIsometricIcon(iconType = iconType, color = accentColor, size = 36.dp)

        Spacer(Modifier.height(8.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontSize = 11.sp,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FourColumnLayout(tool: ToolInfo, accentColor: Color, iconType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ToolIsometricIcon(iconType = iconType, color = accentColor, size = 28.dp)

        Spacer(Modifier.height(4.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 9.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
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
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawLine(
                        color = color,
                        start = Offset(cx - w * 0.25f, cy),
                        end = Offset(cx + w * 0.25f, cy),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx + w * 0.2f, cy - h * 0.15f))
                    drawCircle(color = Violet, radius = 2.dp.toPx(), center = Offset(cx + w * 0.2f, cy + h * 0.15f))
                }
                "nfc" -> {
                    // Isometric Concentric NFC Loops
                    drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = 1.8.dp.toPx()))
                    drawCircle(color = color.copy(alpha = 0.6f), radius = w * 0.25f, style = Stroke(width = 1.4.dp.toPx()))
                    drawCircle(color = color, radius = 2.5.dp.toPx())
                }
                "logs" -> {
                    // Terminal Prompt with Heartbeat pulse
                    val path = Path().apply {
                        moveTo(cx - w * 0.35f, cy - h * 0.2f)
                        lineTo(cx - w * 0.1f, cy)
                        lineTo(cx - w * 0.35f, cy + h * 0.2f)
                    }
                    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    drawLine(
                        color = color,
                        start = Offset(cx + w * 0.05f, cy + h * 0.2f),
                        end = Offset(cx + w * 0.35f, cy + h * 0.2f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                else -> {
                    // Generic Cyber Chip
                    drawRect(color = color, topLeft = Offset(cx - w * 0.3f, cy - h * 0.3f), size = Size(w * 0.6f, h * 0.6f), style = Stroke(2.dp.toPx()))
                    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx, cy))
                }
            }
        }
    }
}