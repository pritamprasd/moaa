package dev.pritam.host.tool.sensors.ui

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.IsometricStatTile
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.tool.sensors.model.SensorCategory
import dev.pritam.host.tool.sensors.model.SensorInfoItem
import dev.pritam.host.tool.sensors.model.SensorValueReading
import dev.pritam.host.tool.sensors.model.UpdateInterval
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

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
                        text = "SENSORS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    GlassBackButton(onClick = onNavigateBack)
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

            Spacer(Modifier.height(4.dp))

            // 2. Search Bar
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
                colors = liquidGlassTextFieldColors(focusedBorderColor = Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // 3. Dropdown Filter Bar: Category Dropdown + Sampling Rate Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryFilterDropdown(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.setSelectedCategory(it) },
                    modifier = Modifier.weight(1f)
                )

                SamplingIntervalDropdown(
                    currentInterval = updateInterval,
                    onSelectInterval = { viewModel.setUpdateInterval(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // 4. Sensors List
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

            LiquidGlassButton(
                onClick = onPauseToggle,
                glowColor = if (isPaused) Cyan else Rose,
                useRainbowBorder = isPaused,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                text = if (isPaused) "▶ Resume" else "⏸ Pause"
            )
        }
    }
}

@Composable
private fun SamplingIntervalDropdown(
    currentInterval: UpdateInterval,
    onSelectInterval: (UpdateInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val intervals = listOf(
        UpdateInterval.EVERY_1_SEC,
        UpdateInterval.EVERY_2_SEC,
        UpdateInterval.EVERY_5_SEC,
        UpdateInterval.LIVE_FAST,
        UpdateInterval.PAUSED
    )

    val currentChipColor = when (currentInterval) {
        UpdateInterval.EVERY_1_SEC -> Color(0xFFF59E0B)
        UpdateInterval.EVERY_2_SEC -> Cyan
        UpdateInterval.EVERY_5_SEC -> Violet
        UpdateInterval.LIVE_FAST -> Color(0xFF34D399)
        UpdateInterval.PAUSED -> Rose
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GlassSurfaceDeep,
            border = BorderStroke(1.dp, if (expanded) currentChipColor else GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = currentChipColor
                    ) {}
                    Text(
                        text = "Rate: ${currentInterval.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = currentChipColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF14161F))
                .border(BorderStroke(1.dp, Color(0xFF222531)), RoundedCornerShape(8.dp))
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
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = chipColor
                            ) {}
                            Text(
                                text = interval.displayName,
                                color = if (isSelected) chipColor else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    },
                    onClick = {
                        onSelectInterval(interval)
                        expanded = false
                    },
                    modifier = Modifier.background(if (isSelected) chipColor.copy(alpha = 0.12f) else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCategory: SensorCategory,
    onSelectCategory: (SensorCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = SensorCategory.entries

    val currentAccentColor = when (selectedCategory) {
        SensorCategory.MOTION -> Color(0xFFF59E0B)
        SensorCategory.ENVIRONMENT -> Cyan
        SensorCategory.POSITION -> Violet
        SensorCategory.HEALTH -> Rose
        else -> Cyan
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GlassSurfaceDeep,
            border = BorderStroke(1.dp, if (expanded) currentAccentColor else GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(selectedCategory.iconLabel, fontSize = 11.sp)
                    Text(
                        text = selectedCategory.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = currentAccentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF14161F))
                .border(BorderStroke(1.dp, Color(0xFF222531)), RoundedCornerShape(8.dp))
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                val catColor = when (cat) {
                    SensorCategory.MOTION -> Color(0xFFF59E0B)
                    SensorCategory.ENVIRONMENT -> Cyan
                    SensorCategory.POSITION -> Violet
                    SensorCategory.HEALTH -> Rose
                    else -> Cyan
                }
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(cat.iconLabel, fontSize = 12.sp)
                            Text(
                                text = cat.displayName,
                                color = if (isSelected) catColor else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    },
                    onClick = {
                        onSelectCategory(cat)
                        expanded = false
                    },
                    modifier = Modifier.background(if (isSelected) catColor.copy(alpha = 0.12f) else Color.Transparent)
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
                .background(Color(0x45030712))
        )

        // Main frosted glass card surface with Rainbow border when active
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
                        if (isActive) RainbowGlassBorderBrush else Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(alpha = 0.4f)))
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

                Spacer(Modifier.height(8.dp))

                // Live Reading Display - Compact & Clean (No extra blank space)
                if (reading != null && isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT READING",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = reading.primaryDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                            Text(
                                text = reading.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp
                            )
                        }
                    }

                    // Multi-axis decomposition (only rendered if non-empty)
                    if (reading.formattedAxes.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                reading.formattedAxes.forEach { (axis, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(axis, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(value, color = TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Inactive / No reading placeholder - compact single line
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isActive) "Waiting for hardware sensor event..." else "Sensor stream disabled. Tap LIVE to start.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Expandable Technical Specifications
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun SensorLiveCardPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        SensorLiveCard(
            sensor = dev.pritam.host.tool.sensors.model.SensorInfoItem(
                id = 1,
                type = 1,
                name = "Linear Accelerometer 3-Axis",
                vendor = "STMicroelectronics",
                version = 1,
                stringType = "android.sensor.accelerometer",
                category = dev.pritam.host.tool.sensors.model.SensorCategory.MOTION,
                maxRange = 78.4f,
                resolution = 0.002f,
                powerMa = 0.25f,
                minDelayUs = 5000,
                fifoMaxEventCount = 0,
                isWakeUp = false,
                isDynamic = false,
                reportingMode = "CONTINUOUS"
            ),
            reading = dev.pritam.host.tool.sensors.model.SensorValueReading(
                sensorType = 1,
                timestampNanos = System.nanoTime(),
                accuracy = 3,
                accuracyLabel = "HIGH",
                rawValues = listOf(0.12f, 9.78f, 0.45f),
                primaryDisplay = "9.81 m/s²",
                formattedAxes = listOf("X" to "+0.12", "Y" to "+9.78", "Z" to "+0.45"),
                unit = "m/s²",
                formattedTime = "22:50:01.120"
            ),
            isExpanded = true,
            isActive = true,
            onToggleExpand = {},
            onToggleActive = {}
        )
    }
}




