package dev.motherofallapps.host.tool.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import dev.motherofallapps.host.tool.sensors.model.SensorCategory
import dev.motherofallapps.host.tool.sensors.model.SensorInfoItem
import dev.motherofallapps.host.tool.sensors.model.SensorValueReading
import dev.motherofallapps.host.tool.sensors.model.UpdateInterval
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.GlassBorder
import dev.motherofallapps.host.ui.theme.GlassBorderHighlight
import dev.motherofallapps.host.ui.theme.GlassSurfaceDeep
import dev.motherofallapps.host.ui.theme.GlassSurfaceElevated
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SensorsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val filteredSensors by viewModel.filteredSensors.collectAsStateWithLifecycle()
    val sensorReadings by viewModel.sensorReadings.collectAsStateWithLifecycle()
    val activeSensorTypes by viewModel.activeSensorTypes.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val updateInterval by viewModel.updateInterval.collectAsStateWithLifecycle()
    val expandedTypes by viewModel.expandedSensorTypes.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SENSORS LIVE",
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
                    TextButton(onClick = { viewModel.exportTelemetry(context) }) {
                        Text("Export", color = Cyan, fontSize = 12.sp)
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
                .padding(horizontal = 16.dp)
        ) {
            // 1. Telemetry Stats Overview
            SensorsStatsBanner(
                stats = stats,
                onPauseToggle = {
                    if (updateInterval == UpdateInterval.PAUSED) {
                        viewModel.resumeAll()
                    } else {
                        viewModel.pauseAll()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // 2. Sampling Rate Interval Selector Bar (1s / 2s / 5s / Live / Pause)
            SamplingIntervalSelector(
                currentInterval = updateInterval,
                onSelectInterval = { viewModel.setUpdateInterval(it) }
            )

            Spacer(Modifier.height(10.dp))

            // 3. Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search sensors by name or vendor...", fontSize = 12.sp) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        TextButton(onClick = { viewModel.setSearchQuery("") }) {
                            Text("Clear", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // 4. Category Filter Chips
            CategoryFilterBar(
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.setSelectedCategory(it) }
            )

            Spacer(Modifier.height(12.dp))

            // 5. Sensors List
            if (filteredSensors.isEmpty()) {
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
                            text = "No hardware sensors found for this filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Try clearing the search query or selecting 'All'",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredSensors, key = { it.type }) { sensor ->
                        val reading = sensorReadings[sensor.type]
                        val isActive = activeSensorTypes.contains(sensor.type)
                        val isExpanded = expandedTypes.contains(sensor.type)

                        SensorLiveCard(
                            sensor = sensor,
                            reading = reading,
                            isActive = isActive,
                            isExpanded = isExpanded,
                            onToggleActive = { viewModel.toggleSensorActive(sensor.type) },
                            onToggleExpand = { viewModel.toggleExpanded(sensor.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorsStatsBanner(
    stats: SensorsStats,
    onPauseToggle: () -> Unit,
) {
    val isPaused = stats.currentInterval == UpdateInterval.PAUSED
    val glowColor = if (isPaused) Rose else Color(0xFFF59E0B)

    IsometricCard(glowColor = glowColor, elevationDepth = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("TOTAL SENSORS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Text("${stats.totalCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("ACTIVE STREAMS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontSize = 9.sp)
                    Text("${stats.activeCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                }
                Column {
                    Text("SAMPLING RATE", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 9.sp)
                    Text(stats.currentInterval.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Cyan)
                }
            }

            OutlinedButton(
                onClick = onPauseToggle,
                border = BorderStroke(1.dp, if (isPaused) Cyan else Rose),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isPaused) Cyan else Rose),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(if (isPaused) "▶ Resume" else "⏸ Pause", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SamplingIntervalSelector(
    currentInterval: UpdateInterval,
    onSelectInterval: (UpdateInterval) -> Unit,
) {
    val intervals = listOf(
        UpdateInterval.EVERY_1_SEC,
        UpdateInterval.EVERY_2_SEC,
        UpdateInterval.EVERY_5_SEC,
        UpdateInterval.LIVE_FAST,
        UpdateInterval.PAUSED
    )

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        intervals.forEach { interval ->
            val isSelected = currentInterval == interval
            val chipColor = when (interval) {
                UpdateInterval.EVERY_1_SEC -> Color(0xFFF59E0B)
                UpdateInterval.EVERY_2_SEC -> Cyan
                UpdateInterval.EVERY_5_SEC -> Violet
                UpdateInterval.LIVE_FAST -> Color(0xFF34D399)
                UpdateInterval.PAUSED -> Rose
            }

            val shape = RoundedCornerShape(8.dp)
            Surface(
                modifier = Modifier
                    .clip(shape)
                    .clickable { onSelectInterval(interval) }
                    .border(
                        BorderStroke(1.dp, if (isSelected) chipColor else GlassBorder),
                        shape
                    ),
                color = if (isSelected) chipColor.copy(alpha = 0.2f) else GlassSurfaceDeep,
                shape = shape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.size(6.dp),
                            shape = CircleShape,
                            color = chipColor
                        ) {}
                    }
                    Text(
                        text = interval.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) chipColor else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selectedCategory: SensorCategory,
    onSelectCategory: (SensorCategory) -> Unit,
) {
    val categories = SensorCategory.entries
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory == cat
            val shape = RoundedCornerShape(8.dp)

            Surface(
                modifier = Modifier
                    .clip(shape)
                    .clickable { onSelectCategory(cat) }
                    .border(
                        BorderStroke(1.dp, if (isSelected) Cyan else GlassBorder),
                        shape
                    ),
                color = if (isSelected) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                shape = shape
            ) {
                Text(
                    text = "${cat.iconLabel} ${cat.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Cyan else TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SensorLiveCard(
    sensor: SensorInfoItem,
    reading: SensorValueReading?,
    isActive: Boolean,
    isExpanded: Boolean,
    onToggleActive: () -> Unit,
    onToggleExpand: () -> Unit,
) {
    val accentColor = when (sensor.category) {
        SensorCategory.MOTION -> Color(0xFFF59E0B) // Amber
        SensorCategory.ENVIRONMENT -> Cyan        // Cyan
        SensorCategory.POSITION -> Violet          // Violet
        SensorCategory.HEALTH -> Rose              // Rose
        else -> Color(0xFF94A3B8)
    }

    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth().animateContentSize(tween(200))) {
        // Refraction depth backplate
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 3.dp)
                .clip(shape)
                .background(Color(0x50030712))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)), shape)
        )

        // Main frosted glass card surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isActive) GlassSurfaceElevated else GlassSurfaceDeep,
                            Color(0x180F172A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                GlassBorderHighlight,
                                if (isActive) accentColor.copy(alpha = 0.7f) else GlassBorder,
                                GlassBorder
                            )
                        )
                    ),
                    shape
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Top Header Row: Category Badge + Sensor Name + Active Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${sensor.category.iconLabel} ${sensor.category.displayName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Column {
                            Text(
                                text = sensor.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${sensor.vendor} · ${sensor.powerMa} mA",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Stream state pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isActive) Color(0xFF34D399).copy(alpha = 0.15f) else Rose.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isActive) Color(0xFF34D399).copy(alpha = 0.4f) else Rose.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onToggleActive() }
                        ) {
                            Text(
                                text = if (isActive) "● LIVE" else "○ OFF",
                                color = if (isActive) Color(0xFF34D399) else Rose,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Expand specs button
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.clickable { onToggleExpand() }
                        ) {
                            Text(
                                text = if (isExpanded) "▲ SPECS" else "▼ SPECS",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Live Reading Display
                if (reading != null && isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "CURRENT READING",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = reading.primaryDisplay,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (reading.accuracy) {
                                    3 -> Color(0xFF34D399).copy(alpha = 0.2f)
                                    2 -> Cyan.copy(alpha = 0.2f)
                                    1 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    else -> Rose.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = reading.accuracyLabel,
                                    color = when (reading.accuracy) {
                                        3 -> Color(0xFF34D399)
                                        2 -> Cyan
                                        1 -> Color(0xFFF59E0B)
                                        else -> Rose
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = reading.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Multi-axis decomposition
                    if (reading.formattedAxes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                reading.formattedAxes.forEach { (axis, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(axis, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(value, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Inactive / No reading placeholder
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isActive) "Waiting for hardware sensor event..." else "Sensor stream disabled. Tap LIVE to start.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Expandable Technical Specifications
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "HARDWARE SPECIFICATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("• Type ID: ${sensor.type} (${sensor.stringType})", color = TextSecondary, fontSize = 9.sp)
                                Text("• Max Range: ${sensor.maxRange} ${reading?.unit ?: ""}", color = TextSecondary, fontSize = 9.sp)
                                Text("• Resolution: ${sensor.resolution} ${reading?.unit ?: ""}", color = TextSecondary, fontSize = 9.sp)
                                Text("• Power Consumption: ${sensor.powerMa} mA", color = TextSecondary, fontSize = 9.sp)
                                Text("• Min Delay: ${sensor.minDelayUs} µs", color = TextSecondary, fontSize = 9.sp)
                                Text("• Reporting Mode: ${sensor.reportingMode}", color = TextSecondary, fontSize = 9.sp)
                                Text("• Wake-Up: ${if (sensor.isWakeUp) "Yes" else "No"} | Dynamic: ${if (sensor.isDynamic) "Yes" else "No"}", color = TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
