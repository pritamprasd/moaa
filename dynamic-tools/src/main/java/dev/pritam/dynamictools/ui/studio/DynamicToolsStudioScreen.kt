package dev.pritam.dynamictools.ui.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolPreset
import dev.pritam.dynamictools.ui.components.DynamicToolCodeEditorDialog
import dev.pritam.dynamictools.ui.theme.Cyan
import dev.pritam.dynamictools.ui.theme.Emerald
import dev.pritam.dynamictools.ui.theme.GlassBorder
import dev.pritam.dynamictools.ui.theme.RainbowGlassBorderBrush
import dev.pritam.dynamictools.ui.theme.Rose
import dev.pritam.dynamictools.ui.theme.SpaceBackground
import dev.pritam.dynamictools.ui.theme.SurfaceDeep
import dev.pritam.dynamictools.ui.theme.SurfaceElevated
import dev.pritam.dynamictools.ui.theme.TextPrimary
import dev.pritam.dynamictools.ui.theme.TextSecondary
import dev.pritam.dynamictools.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicToolsStudioScreen(
    onNavigateBack: () -> Unit,
    onLaunchTool: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DynamicToolsStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val installedTools by viewModel.installedTools.collectAsStateWithLifecycle()
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationStatus by viewModel.generationStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var editingBundle by remember { mutableStateOf<DynamicToolBundle?>(null) }
    var toolToRefine by remember { mutableStateOf<DynamicToolBundle?>(null) }
    var toolToDelete by remember { mutableStateOf<DynamicToolBundle?>(null) }
    val isRefining by viewModel.isRefining.collectAsStateWithLifecycle()
    val refinementStatus by viewModel.refinementStatus.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DYNAMIC TOOLS STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${installedTools.size} Active Web Tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Import JSON Tool Action
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📥", fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Hero Generator Card
            item {
                GeneratorPromptCard(
                    prompt = promptInput,
                    isGenerating = isGenerating,
                    generationStatus = generationStatus,
                    errorMessage = errorMessage,
                    onPromptChange = { viewModel.setPrompt(it) },
                    onSelectPreset = { viewModel.applyPreset(it) },
                    onGenerate = {
                        viewModel.generateTool { toolId ->
                            onLaunchTool(toolId)
                        }
                    },
                    onClearError = { viewModel.clearError() }
                )
            }

            // 2. Installed Tools Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTALLED DYNAMIC WEB TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Cyan,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Cyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "SANDBOXED HTML/JS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. List of Installed Tools
            if (installedTools.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛠️", fontSize = 28.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No Dynamic Tools Generated Yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Type a prompt above or pick a preset to synthesize an interactive web app via LLM Gateway.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(installedTools, key = { it.manifest.toolId }) { bundle ->
                    DynamicToolItemCard(
                        bundle = bundle,
                        onLaunch = { onLaunchTool(bundle.manifest.toolId) },
                        onRefinePrompt = { toolToRefine = bundle },
                        onEditCode = { editingBundle = bundle },
                        onShareJson = {
                            val json = viewModel.exportToolJson(bundle.manifest.toolId)
                            if (json != null) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Dynamic Tool JSON", json))
                                Toast.makeText(context, "Exported '${bundle.manifest.displayName}' JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = { toolToDelete = bundle }
                    )
                }
            }
        }
    }

    // AI Refine Prompt Dialog
    if (toolToRefine != null) {
        dev.pritam.dynamictools.ui.components.DynamicToolRefineDialog(
            bundle = toolToRefine!!,
            isRefining = isRefining,
            refinementStatus = refinementStatus,
            errorMessage = errorMessage,
            onDismiss = {
                toolToRefine = null
                viewModel.clearError()
            },
            onRefine = { prompt ->
                viewModel.refineTool(toolToRefine!!, prompt) {
                    Toast.makeText(context, "Tool updated successfully!", Toast.LENGTH_SHORT).show()
                    toolToRefine = null
                }
            }
        )
    }

    // Code Editor Modal
    if (editingBundle != null) {
        DynamicToolCodeEditorDialog(
            bundle = editingBundle!!,
            onDismiss = { editingBundle = null },
            onSave = { html, css, js ->
                // Save and reload
                editingBundle = null
            }
        )
    }

    // Delete Confirmation
    if (toolToDelete != null) {
        AlertDialog(
            onDismissRequest = { toolToDelete = null },
            containerColor = SurfaceDeep,
            title = {
                Text(
                    text = "Delete Dynamic Tool?",
                    fontWeight = FontWeight.Bold,
                    color = Rose,
                    fontSize = 14.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${toolToDelete?.manifest?.displayName}'? Its sandboxed HTML, CSS, and JS files will be permanently removed.",
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        toolToDelete?.let { viewModel.deleteTool(it.manifest.toolId) }
                        toolToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toolToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = SurfaceDeep,
            title = {
                Text(
                    text = "Import Tool from JSON",
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a valid Dynamic Tool JSON bundle below:",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("{\"manifest\": {...}, \"html\": \"...\"}", fontSize = 10.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0xFF030712),
                            unfocusedContainerColor = Color(0xFF030712)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            val success = viewModel.importToolJson(importJsonText)
                            if (success) {
                                Toast.makeText(context, "Tool imported successfully!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                                importJsonText = ""
                            } else {
                                Toast.makeText(context, "Invalid Tool JSON bundle", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Import", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GeneratorPromptCard(
    prompt: String,
    isGenerating: Boolean,
    generationStatus: String?,
    errorMessage: String?,
    onPromptChange: (String) -> Unit,
    onSelectPreset: (DynamicToolPreset) -> Unit,
    onGenerate: () -> Unit,
    onClearError: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDeep,
        border = BorderStroke(1.dp, RainbowGlassBorderBrush),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI WEB TOOL GENERATOR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Cyan,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Describe your tool in English and synthesize instantly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Preset Chips Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DynamicToolPreset.PRESETS.forEach { preset ->
                    Surface(
                        onClick = { onSelectPreset(preset) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x251E293B),
                        border = BorderStroke(1.dp, Color(preset.accentColorHex).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(preset.accentColorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Prompt Input Field
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = "e.g. Build an interactive Unit Converter supporting Length, Weight, Temperature, and Speed with swap buttons...",
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = Color(0xFF030712),
                    unfocusedContainerColor = Color(0xFF030712),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isGenerating
            )

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x25EF4444),
                    border = BorderStroke(1.dp, Rose),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Error: $errorMessage",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = Rose, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Loading / Generating State
            AnimatedVisibility(
                visible = isGenerating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x2038BDF8),
                    border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Cyan,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = generationStatus ?: "Synthesizing HTML/CSS/JS with LLM Gateway...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = onGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan,
                    disabledContainerColor = Color(0x4038BDF8)
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isGenerating && prompt.isNotBlank()
            ) {
                Text(
                    text = if (isGenerating) "GENERATING DYNAMIC WEB APP..." else "⚡ SYNTHESIZE & LAUNCH WEB APP",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun DynamicToolItemCard(
    bundle: DynamicToolBundle,
    onLaunch: () -> Unit,
    onRefinePrompt: () -> Unit,
    onEditCode: () -> Unit,
    onShareJson: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = Color(bundle.manifest.accentColorHex)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Icon + Title + Version + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val iconEmoji = when (bundle.manifest.iconName) {
                                "calculator" -> "🧮"
                                "convert" -> "🔄"
                                "code" -> "💻"
                                "timer" -> "⏱️"
                                "terminal" -> "⌨️"
                                "chart" -> "📊"
                                else -> "🛠️"
                            }
                            Text(iconEmoji, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = bundle.manifest.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "v${bundle.manifest.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "· ${bundle.manifest.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Rose.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = bundle.manifest.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Launch Button
                Button(
                    onClick = onLaunch,
                    modifier = Modifier.weight(1.3f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Launch",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Launch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Refine with AI Prompt Button
                OutlinedButton(
                    onClick = onRefinePrompt,
                    modifier = Modifier.weight(1.1f).height(34.dp),
                    border = BorderStroke(1.dp, Violet.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("✨ Refine", color = Violet, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                // Edit Code Button
                OutlinedButton(
                    onClick = onEditCode,
                    modifier = Modifier.weight(0.9f).height(34.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("</> Code", color = TextPrimary, fontSize = 10.sp)
                }

                // Share JSON Button
                OutlinedButton(
                    onClick = onShareJson,
                    modifier = Modifier.weight(0.9f).height(34.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📤 Export", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}
