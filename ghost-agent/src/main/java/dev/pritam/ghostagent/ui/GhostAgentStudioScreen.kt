package dev.pritam.ghostagent.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pritam.ghostagent.GhostAgentManager
import dev.pritam.ghostagent.model.ExecutionStatus
import dev.pritam.ghostagent.model.GhostExecutionState
import dev.pritam.ghostagent.model.GhostStep
import dev.pritam.ghostagent.model.GhostTask
import dev.pritam.ghostagent.model.StepType
import dev.pritam.ghostagent.storage.GhostTaskRepository
import kotlinx.coroutines.launch
import java.util.UUID

// ─── Color Tokens (reuses MOAA theme values) ─────────────────────────────────
private val BgDeep     = Color(0xFF030712)
private val GlassCard  = Color(0x661E293B)
private val GlassBorder = Color(0x1F94A3B8)
private val CyanAccent = Color(0xFF38BDF8)
private val VioletAccent = Color(0xFFA78BFA)
private val RoseAccent = Color(0xFFF472B6)
private val EmeraldAccent = Color(0xFF34D399)
private val AmberAccent = Color(0xFFF59E0B)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val TextTertiary = Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostAgentStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { GhostTaskRepository.getInstance(context) }
    val tasks by repo.tasks.collectAsStateWithLifecycle()
    val executionState by GhostAgentManager.executionState.collectAsStateWithLifecycle()
    val isA11yConnected = GhostAgentManager.isAccessibilityServiceConnected()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<GhostTask?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👻", fontSize = 20.sp)
                        Text(
                            "GHOST AGENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Task", tint = CyanAccent, modifier = Modifier.size(26.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Status / Permission Banner ──────────────────────────────────
            item {
                AccessibilityStatusCard(isConnected = isA11yConnected)
            }

            // ── Active Execution Card ───────────────────────────────────────
            if (executionState.status != ExecutionStatus.IDLE && executionState.status != ExecutionStatus.COMPLETED) {
                item {
                    ExecutionProgressCard(state = executionState, onAbort = {
                        GhostAgentManager.abortCurrentTask()
                    })
                }
            }

            // ── Overlay Controls ────────────────────────────────────────────
            item {
                OverlayControlsCard(context = context)
            }

            // ── Task List ───────────────────────────────────────────────────
            if (tasks.isEmpty()) {
                item {
                    EmptyTasksPlaceholder(onCreate = { showCreateDialog = true })
                }
            } else {
                items(tasks, key = { it.taskId }) { task ->
                    GhostTaskCard(
                        task = task,
                        isRunning = executionState.taskId == task.taskId && executionState.status == ExecutionStatus.RUNNING,
                        onRun = {
                            if (isA11yConnected) GhostAgentManager.executeTask(context, task.taskId)
                        },
                        onEdit = { selectedTask = task },
                        onDelete = {
                            scope.launch { repo.deleteTask(task.taskId) }
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { newTask ->
                scope.launch { repo.saveTask(newTask) }
                showCreateDialog = false
            }
        )
    }

    selectedTask?.let { task ->
        TaskEditorSheet(
            task = task,
            onDismiss = { selectedTask = null },
            onSave = { updated ->
                scope.launch { repo.saveTask(updated) }
                selectedTask = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accessibility Status Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AccessibilityStatusCard(isConnected: Boolean) {
    val color = if (isConnected) EmeraldAccent else AmberAccent
    val text = if (isConnected) "✓ Accessibility Service Connected" else "⚠ Accessibility Service Not Connected"
    val sub = if (isConnected) "Ghost Agent can read screen and dispatch gestures."
              else "Go to Settings → Accessibility → Ghost Agent and enable it."

    GlassCard(glowColor = color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isConnected) "🟢" else "🟡", fontSize = 18.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Execution Progress Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExecutionProgressCard(state: GhostExecutionState, onAbort: () -> Unit) {
    val statusColor = when (state.status) {
        ExecutionStatus.RUNNING       -> CyanAccent
        ExecutionStatus.STEP_FAILED   -> RoseAccent
        ExecutionStatus.WAITING_USER  -> AmberAccent
        ExecutionStatus.PAUSED        -> VioletAccent
        else                          -> TextSecondary
    }
    val progress = if (state.totalSteps > 0) (state.currentStepIndex + 1).toFloat() / state.totalSteps else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = spring(Spring.DampingRatioMediumBouncy))

    GlassCard(glowColor = statusColor) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    StatusChip(label = state.status.name, color = statusColor)
                    Spacer(Modifier.height(4.dp))
                    Text(state.taskName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        "Step ${(state.currentStepIndex + 1).coerceAtLeast(1)}/${state.totalSteps}: ${state.currentStepLabel}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoseAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable { onAbort() }
                ) {
                    Text("Abort", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = RoseAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
            if (state.errorMessage.isNotBlank()) {
                Text("⚠ ${state.errorMessage}", style = MaterialTheme.typography.bodySmall, color = RoseAccent, fontSize = 10.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay Controls Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverlayControlsCard(context: android.content.Context) {
    val isBubbleVisible = GhostAgentManager.isBubbleVisible()

    GlassCard(glowColor = VioletAccent) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelText("OVERLAY CONTROLS")
            Text("Show a floating bubble or add a Quick Settings tile for instant access from any app.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentButton(
                    text = if (isBubbleVisible) "Hide Bubble" else "Show Bubble 👻",
                    color = VioletAccent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isBubbleVisible) GhostAgentManager.hideFloatingBubble(context)
                        else GhostAgentManager.showFloatingBubble(context)
                    }
                )
                AccentButton(
                    text = "Open A11y Settings",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ghost Task Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GhostTaskCard(
    task: GhostTask,
    isRunning: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accentColor = if (isRunning) CyanAccent else VioletAccent
    val shape = RoundedCornerShape(14.dp)

    Surface(
        shape = shape,
        color = GlassCard,
        border = BorderStroke(1.dp, if (isRunning) CyanAccent.copy(alpha = 0.5f) else GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👻", fontSize = 16.sp)
                        Text(task.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isRunning) StatusChip("RUNNING", CyanAccent)
                    }
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(task.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseAccent.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Step preview
            if (task.steps.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0x1A0F172A)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.steps.take(3).forEachIndexed { i, step ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = accentColor.copy(alpha = 0.15f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    step.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StepTypeTag(step.type)
                            }
                        }
                        if (task.steps.size > 3) {
                            Text("+ ${task.steps.size - 3} more steps", color = TextTertiary, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton(text = "Edit", color = VioletAccent, modifier = Modifier.weight(1f), onClick = onEdit)
                AccentButton(
                    text = if (isRunning) "Running…" else "▶ Run",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    onClick = onRun
                )
            }

            // Run count & app
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("${task.steps.size} steps")
                MetaChip("Ran ${task.runCount}×")
                if (task.targetAppPackage.isNotBlank()) MetaChip("🔗 ${task.targetAppPackage.substringAfterLast('.')}")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Create Task Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CreateTaskDialog(onDismiss: () -> Unit, onCreate: (GhostTask) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetApp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("New Ghost Task", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassTextField("Task Name", name) { name = it }
                GlassTextField("Description (optional)", description) { description = it }
                GlassTextField("Target App Package (optional, e.g. com.amazon.mShop)", targetApp) { targetApp = it }
                Text(
                    "Steps can be added after creation in the task editor.",
                    color = TextTertiary, fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    onCreate(GhostTask(
                        taskId = UUID.randomUUID().toString(),
                        name = name.trim(),
                        description = description.trim(),
                        targetAppPackage = targetApp.trim(),
                    ))
                }
            ) { Text("Create", color = CyanAccent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Editor Sheet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskEditorSheet(task: GhostTask, onDismiss: () -> Unit, onSave: (GhostTask) -> Unit) {
    var name by remember { mutableStateOf(task.name) }
    var description by remember { mutableStateOf(task.description) }
    var targetApp by remember { mutableStateOf(task.targetAppPackage) }
    var steps by remember { mutableStateOf(task.steps) }
    var showAddStep by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(vertical = 16.dp),
        title = {
            Text("Edit Task", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { GlassTextField("Task Name", name) { name = it } }
                item { GlassTextField("Description", description) { description = it } }
                item { GlassTextField("Target App Package", targetApp) { targetApp = it } }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("STEPS (${steps.size})", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        AccentButton("+ Add Step", CyanAccent, onClick = { showAddStep = true })
                    }
                }

                itemsIndexed(steps) { idx, step ->
                    StepEditorRow(
                        index = idx,
                        step = step,
                        onUpdate = { updated -> steps = steps.toMutableList().also { it[idx] = updated } },
                        onDelete = { steps = steps.filterIndexed { i, _ -> i != idx } }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(task.copy(name = name.trim(), description = description.trim(), targetAppPackage = targetApp.trim(), steps = steps))
            }) { Text("Save", color = EmeraldAccent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )

    if (showAddStep) {
        AddStepDialog(
            onDismiss = { showAddStep = false },
            onAdd = { newStep ->
                steps = steps + newStep
                showAddStep = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step Editor Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepEditorRow(index: Int, step: GhostStep, onUpdate: (GhostStep) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val stepColor = when (step.type) {
        StepType.NATURAL_LANGUAGE -> CyanAccent
        StepType.RECORDED_MACRO   -> VioletAccent
        StepType.COMPOSITE        -> EmeraldAccent
    }

    Surface(shape = RoundedCornerShape(10.dp), color = Color(0x1A0F172A), border = BorderStroke(1.dp, stepColor.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(22.dp), shape = CircleShape, color = stepColor.copy(0.15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = stepColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(step.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(step.type.name, color = stepColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent.copy(0.5f), modifier = Modifier.size(14.dp))
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField("Label", step.label) { onUpdate(step.copy(label = it)) }
                    if (step.type != StepType.RECORDED_MACRO) {
                        GlassTextField("NL Instruction (e.g. tap 'Add to Cart')", step.nlPrompt) { onUpdate(step.copy(nlPrompt = it)) }
                    }
                    if (step.type == StepType.RECORDED_MACRO || step.type == StepType.COMPOSITE) {
                        Text("${step.events.size} recorded events", color = VioletAccent, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Step Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddStepDialog(onDismiss: () -> Unit, onAdd: (GhostStep) -> Unit) {
    var label by remember { mutableStateOf("") }
    var nlPrompt by remember { mutableStateOf("") }
    var stepType by remember { mutableStateOf(StepType.NATURAL_LANGUAGE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(18.dp),
        title = { Text("Add Step", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField("Step Label", label) { label = it }

                // Step type selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepType.values().forEach { type ->
                        val isSelected = type == stepType
                        val color = when (type) {
                            StepType.NATURAL_LANGUAGE -> CyanAccent
                            StepType.RECORDED_MACRO   -> VioletAccent
                            StepType.COMPOSITE        -> EmeraldAccent
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) color else GlassBorder),
                            modifier = Modifier.weight(1f).clickable { stepType = type }
                        ) {
                            Text(
                                type.name.replace("_", "\n"),
                                modifier = Modifier.padding(6.dp),
                                color = if (isSelected) color else TextSecondary,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (stepType != StepType.RECORDED_MACRO) {
                    GlassTextField(
                        hint = when (stepType) {
                            StepType.NATURAL_LANGUAGE -> "e.g. tap 'Search', type 'iPhone charger', scroll down"
                            StepType.COMPOSITE -> "NL fallback instruction (e.g. tap 'Checkout')"
                            else -> "NL Instruction"
                        },
                        value = nlPrompt,
                        onValueChange = { nlPrompt = it }
                    )
                }

                if (stepType == StepType.RECORDED_MACRO || stepType == StepType.COMPOSITE) {
                    Text(
                        "📝 Macro recording will be available when you run the task with the recorder enabled. " +
                        "For now, add the step and record events later.",
                        color = TextTertiary, fontSize = 10.sp, lineHeight = 14.sp
                    )
                }

                // NL Quick Reference
                if (stepType != StepType.RECORDED_MACRO) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0x0F38BDF8)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("NL SYNTAX REFERENCE", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            listOf(
                                "open <app name>" to "Launch an app",
                                "tap '<button text>'" to "Tap UI element by text",
                                "tap id '<view_id>'" to "Tap by resource ID",
                                "type '<text>'" to "Type into focused field",
                                "scroll down / scroll up" to "Scroll the screen",
                                "back | home" to "Press Back / Home",
                                "wait 3s" to "Wait 3 seconds",
                            ).forEach { (cmd, desc) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(cmd, color = CyanAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(160.dp))
                                    Text("→ $desc", color = TextTertiary, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank()) return@TextButton
                onAdd(GhostStep(
                    stepId = UUID.randomUUID().toString(),
                    type = stepType,
                    label = label.trim(),
                    nlPrompt = nlPrompt.trim(),
                ))
            }) { Text("Add Step", color = CyanAccent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyTasksPlaceholder(onCreate: () -> Unit) {
    GlassCard(glowColor = VioletAccent) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("👻", fontSize = 48.sp)
            Text("No Automation Tasks", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Create your first Ghost Task to automate multi-step workflows across any app.", color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
            AccentButton("+ Create First Task", VioletAccent, onClick = onCreate)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI Primitives
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassCard(glowColor: Color = CyanAccent, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassCard,
        border = BorderStroke(1.dp, glowColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun AccentButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f * alpha),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f * alpha)),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = color.copy(alpha = alpha), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color(0x1A475569)) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StepTypeTag(type: StepType) {
    val (label, color) = when (type) {
        StepType.NATURAL_LANGUAGE -> "NL" to CyanAccent
        StepType.RECORDED_MACRO   -> "REC" to VioletAccent
        StepType.COMPOSITE        -> "MIX" to EmeraldAccent
    }
    Surface(shape = RoundedCornerShape(3.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LabelText(text: String) {
    Text(text, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTextField(hint: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint, color = TextTertiary, fontSize = 13.sp) },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x1A0F172A),
            unfocusedContainerColor = Color(0x0F0F172A),
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanAccent,
        ),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
        singleLine = false,
        maxLines = 3,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun GhostTaskCardPreview() {
    MaterialTheme {
        GhostTaskCard(
            task = GhostTask(
                taskId = "task_insta_scroll",
                name = "Auto Scroll & Like",
                description = "Automatically opens target app, scrolls feeds and triggers interactions via accessibility automation.",
                targetAppPackage = "com.instagram.android",
                steps = listOf(
                    GhostStep(
                        stepId = "step_1",
                        label = "Open Feed",
                        type = StepType.NATURAL_LANGUAGE,
                        nlPrompt = "Scroll down by 500px and double-tap photo"
                    )
                ),
                createdAt = System.currentTimeMillis()
            ),
            isRunning = false,
            onRun = {},
            onEdit = {},
            onDelete = {}
        )
    }
}


