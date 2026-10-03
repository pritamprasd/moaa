package dev.pritam.host.tool.terminal.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.settings.AppSettingsManager
import dev.pritam.host.tool.terminal.model.OutputLineType
import dev.pritam.host.tool.terminal.model.SavedCommand
import dev.pritam.host.tool.terminal.model.TerminalOutputLine
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
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.TextTertiary
import dev.pritam.host.ui.theme.Violet
import kotlinx.coroutines.launch

private val ConsoleBackground = Color(0xFF060B12)
private val ConsolePromptColor = Color(0xFF34D399) // Matrix Emerald
private val ConsoleCommandColor = Color(0xFF38BDF8) // Cyan
private val ConsoleStdoutColor = Color(0xFFE2E8F0) // Off-white
private val ConsoleStderrColor = Color(0xFFF43F5E) // Rose Neon
private val ConsoleSystemColor = Color(0xFFF59E0B) // Amber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val outputLines by viewModel.outputLines.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val activeCommand by viewModel.activeCommand.collectAsStateWithLifecycle()
    val savedCommands by viewModel.savedCommands.collectAsStateWithLifecycle()
    val inputCommand by viewModel.inputCommand.collectAsStateWithLifecycle()
    val workingDir by viewModel.workingDir.collectAsStateWithLifecycle()

    var showSavedCommandsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingSaveCommandText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new output
    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CYBER TERMINAL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald,
                            letterSpacing = 1.sp
                        )

                        // Working directory badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Emerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Emerald.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "📁 ${viewModel.getShortWorkingDir()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = { GlassBackButton(onClick = onNavigateBack) },
                actions = {
                    // Stop active process button
                    AnimatedVisibility(visible = isRunning, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Rose.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Rose),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.cancel() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("⏹", fontSize = 10.sp, color = Rose)
                                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose)
                            }
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Saved Snippets Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Cyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showSavedCommandsSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡", fontSize = 11.sp)
                            Text("Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cyan)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Copy All Output Button
                    IconButton(onClick = {
                        val text = viewModel.getAllOutputAsText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", text))
                        Toast.makeText(context, "Terminal output copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("📋", fontSize = 14.sp)
                    }

                    // Clear Screen Button
                    IconButton(onClick = { viewModel.clear() }) {
                        Text("🗑️", fontSize = 14.sp)
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
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // ── QUICK ACCESS SAVED COMMANDS CHIPS CAROUSEL ──────────────────
            QuickSavedChipsRow(
                savedCommands = savedCommands,
                onRunCommand = { cmd -> viewModel.execute(cmd) },
                onLoadCommand = { cmd -> viewModel.setInput(cmd) },
                onOpenSavedSheet = { showSavedCommandsSheet = true }
            )

            Spacer(Modifier.height(6.dp))

            // ── TERMINAL CONSOLE OUTPUT BOX ────────────────────────────────
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ConsoleBackground,
                border = BorderStroke(1.dp, RainbowGlassBorderBrush),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(outputLines, key = { it.id }) { line ->
                            TerminalLineItem(line = line)
                        }

                        if (isRunning) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Emerald,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Text(
                                        text = "Executing: ${activeCommand ?: "..."}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Emerald,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── VIRTUAL ACCESSORY KEYBOARD ROW ─────────────────────────────
            VirtualAccessoryRow(
                onTokenClick = { token -> viewModel.appendToken(token) },
                onHistoryUp = { viewModel.navigateHistoryUp() },
                onHistoryDown = { viewModel.navigateHistoryDown() }
            )

            Spacer(Modifier.height(6.dp))

            // ── COMMAND INPUT BAR ──────────────────────────────────────────
            CommandInputBar(
                inputCommand = inputCommand,
                isRunning = isRunning,
                onInputChange = { viewModel.setInput(it) },
                onExecute = { viewModel.execute() },
                onCancel = { viewModel.cancel() },
                onSaveCurrent = {
                    pendingSaveCommandText = inputCommand
                    showSaveDialog = true
                }
            )

            Spacer(Modifier.height(4.dp))
        }
    }

    // ── SAVED COMMANDS BOTTOM SHEET ────────────────────────────────────────
    if (showSavedCommandsSheet) {
        SavedCommandsBottomSheet(
            savedCommands = savedCommands,
            onDismiss = { showSavedCommandsSheet = false },
            onRunCommand = { cmd ->
                showSavedCommandsSheet = false
                viewModel.execute(cmd)
            },
            onLoadCommand = { cmd ->
                showSavedCommandsSheet = false
                viewModel.setInput(cmd)
            },
            onDeleteCommand = { id -> viewModel.deleteSavedCommand(id) },
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            onAddNewCommand = {
                pendingSaveCommandText = ""
                showSaveDialog = true
            },
            onResetDefaults = { viewModel.resetSavedCommandsToDefaults() }
        )
    }

    // ── ADD / SAVE COMMAND DIALOG ──────────────────────────────────────────
    if (showSaveDialog) {
        SaveCommandDialog(
            initialCommand = pendingSaveCommandText,
            onDismiss = { showSaveDialog = false },
            onSave = { name, cmd, category, desc ->
                viewModel.saveCommand(name, cmd, category, desc)
                showSaveDialog = false
                Toast.makeText(context, "Saved command '$name'!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun TerminalLineItem(line: TerminalOutputLine) {
    when (line.type) {
        OutputLineType.COMMAND -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = line.text,
                    color = ConsoleCommandColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        OutputLineType.STDOUT -> {
            Text(
                text = line.text,
                color = ConsoleStdoutColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        OutputLineType.STDERR -> {
            Text(
                text = line.text,
                color = ConsoleStderrColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        OutputLineType.SUCCESS -> {
            Text(
                text = line.text,
                color = ConsolePromptColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
        OutputLineType.ERROR -> {
            Text(
                text = line.text,
                color = Rose,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
        OutputLineType.SYSTEM_INFO -> {
            Text(
                text = line.text,
                color = ConsoleSystemColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun QuickSavedChipsRow(
    savedCommands: List<SavedCommand>,
    onRunCommand: (String) -> Unit,
    onLoadCommand: (String) -> Unit,
    onOpenSavedSheet: () -> Unit
) {
    val scrollState = rememberScrollState()
    val topItems = savedCommands.filter { it.isFavorite }.ifEmpty { savedCommands.take(6) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "+ All Saved" button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Violet.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Violet.copy(alpha = 0.35f)),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onOpenSavedSheet() }
        ) {
            Text(
                text = "⚡ Snippets (${savedCommands.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Violet,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }

        topItems.forEach { item ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, Emerald.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onRunCommand(item.command) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("▶", fontSize = 8.sp, color = Emerald)
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VirtualAccessoryRow(
    onTokenClick: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit
) {
    val scrollState = rememberScrollState()
    val keys = listOf("Tab", "|", "-", "/", "~", "&", "$", ">", "\"", "'", "curl", "clear", "grep", "cat", "ps")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // History Up / Down navigation keys
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = GlassSurfaceElevated,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onHistoryUp() }
        ) {
            Text(
                text = "▲ Prev",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = GlassSurfaceElevated,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onHistoryDown() }
        ) {
            Text(
                text = "▼ Next",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        keys.forEach { key ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onTokenClick(key) }
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CommandInputBar(
    inputCommand: String,
    isRunning: Boolean,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    onCancel: () -> Unit,
    onSaveCurrent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (isRunning) Rose.copy(alpha = 0.5f) else Emerald.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                color = Emerald,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            TextField(
                value = inputCommand,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "curl, ping, ls, ip addr, help...",
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Emerald,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onExecute() }),
                singleLine = true
            )

            // Save button
            IconButton(
                onClick = onSaveCurrent,
                enabled = inputCommand.isNotBlank(),
                modifier = Modifier.size(32.dp)
            ) {
                Text("💾", fontSize = 14.sp)
            }

            Spacer(Modifier.width(4.dp))

            // Execute / Stop button
            if (isRunning) {
                LiquidGlassButton(
                    onClick = onCancel,
                    glowColor = Rose,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("⏹ Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Rose)
                }
            } else {
                LiquidGlassButton(
                    onClick = onExecute,
                    glowColor = Emerald,
                    useRainbowBorder = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("▶ Run", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedCommandsBottomSheet(
    savedCommands: List<SavedCommand>,
    onDismiss: () -> Unit,
    onRunCommand: (String) -> Unit,
    onLoadCommand: (String) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddNewCommand: () -> Unit,
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(savedCommands) {
        listOf("All") + savedCommands.map { it.category }.distinct().sorted()
    }

    val filteredList = remember(savedCommands, searchQuery, selectedCategory) {
        savedCommands.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.command.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSettingsManager.getDialogSurfaceColor(96)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAVED COMMANDS & SNIPPETS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "1-click execution for APIs, curl requests & diagnostic commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                LiquidGlassButton(
                    onClick = onAddNewCommand,
                    glowColor = Emerald,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+ Add New", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search saved commands (e.g. ollama, ping, curl)...", fontSize = 11.sp, color = TextTertiary) },
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

            Spacer(Modifier.height(8.dp))

            // Category filter chips
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                        border = BorderStroke(1.dp, if (isSelected) Cyan else GlassBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Cyan else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Command Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { cmd ->
                    SavedCommandCard(
                        cmd = cmd,
                        onRun = { onRunCommand(cmd.command) },
                        onLoad = { onLoadCommand(cmd.command) },
                        onDelete = { onDeleteCommand(cmd.id) },
                        onToggleFavorite = { onToggleFavorite(cmd.id) }
                    )
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved commands match your filter",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onResetDefaults) {
                            Text("Reset Command Snippets to Defaults", fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCommandCard(
    cmd: SavedCommand,
    onRun: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, if (cmd.isFavorite) Cyan.copy(alpha = 0.5f) else GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (cmd.isFavorite) "★" else "☆",
                        fontSize = 14.sp,
                        color = if (cmd.isFavorite) Amber else TextTertiary,
                        modifier = Modifier.clickable { onToggleFavorite() }
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Cyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = cmd.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = cmd.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Load to prompt button
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GlassSurfaceElevated,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onLoad() }
                    ) {
                        Text(
                            text = "✏️ Load",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Run Now Button
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Emerald.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Emerald),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onRun() }
                    ) {
                        Text(
                            text = "▶ Run",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Text("🗑️", fontSize = 10.sp)
                    }
                }
            }

            if (cmd.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = cmd.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            // Code snippet container
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ConsoleBackground,
                border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cmd.command,
                    color = ConsolePromptColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SaveCommandDialog(
    initialCommand: String,
    onDismiss: () -> Unit,
    onSave: (name: String, command: String, category: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf(initialCommand) }
    var category by remember { mutableStateOf("Custom") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppSettingsManager.getDialogSurfaceColor(98),
            border = BorderStroke(1.dp, RainbowGlassBorderBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SAVE TERMINAL COMMAND",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Save this command to run directly anytime without retyping.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(12.dp))

                // Name input
                Text("Command Name *", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Ollama Tags Query", fontSize = 11.sp, color = TextTertiary) },
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

                Spacer(Modifier.height(8.dp))

                // Command text input
                Text("Shell Command *", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("e.g. curl http://127.0.0.1:11434/api/tags", fontSize = 11.sp, color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Category chips
                Text("Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("AI & LLM", "Networking", "System", "Storage", "Hardware", "Custom").forEach { cat ->
                        val isSel = category == cat
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) Cyan.copy(alpha = 0.2f) else GlassSurfaceDeep,
                            border = BorderStroke(1.dp, if (isSel) Cyan else GlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) Cyan else TextSecondary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Description input
                Text("Description (Optional)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("What does this command do?", fontSize = 11.sp, color = TextTertiary) },
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

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    LiquidGlassButton(
                        onClick = {
                            if (name.isNotBlank() && command.isNotBlank()) {
                                onSave(name, command, category, description)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        glowColor = Cyan,
                        useRainbowBorder = true,
                        text = "Save Snippet"
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun TerminalScreenPreview() {
    AppTheme {
        TerminalScreen(onNavigateBack = {})
    }
}
