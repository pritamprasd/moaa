package dev.pritam.host.tool.llmchat.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.tool.llmchat.model.PersonaPreset
import dev.pritam.host.tool.llmchat.ui.components.ChatMessageBubble
import dev.pritam.host.tool.llmchat.ui.components.ChatSessionDrawer
import dev.pritam.host.tool.llmchat.ui.components.PersonaConfigDialog
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LlmChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val inputPrompt by viewModel.inputPrompt.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val serverTelemetry by viewModel.serverTelemetry.collectAsStateWithLifecycle()
    val gatewayProfiles by viewModel.gatewayProfiles.collectAsStateWithLifecycle()
    val mcpServers by viewModel.mcpServers.collectAsStateWithLifecycle()

    var showSessionsDrawer by remember { mutableStateOf(false) }
    var showPersonaDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val messages = activeSession?.messages ?: emptyList()

    // Auto scroll to bottom when messages update
    LaunchedEffect(messages.size, if (messages.isNotEmpty()) messages.last().content.length else 0) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val activePersona = PersonaPreset.findById(activeSession?.personaId ?: PersonaPreset.DEFAULT_ASSISTANT.id)
    val enabledProfilesCount = gatewayProfiles.count { it.isEnabled }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeSession?.title ?: "CYBERCHAT AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${activePersona.icon} ${activePersona.name} · ${if (serverTelemetry.isRunning) "● Gateway Active" else "○ Gateway Standby"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose,
                            fontSize = 9.sp
                        )
                    }
                },
                navigationIcon = {
                    GlassBackButton(onClick = onNavigateBack)
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { showSessionsDrawer = true }
                        ) {
                            Text("💬 Chats", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.clickable { showPersonaDialog = true }
                        ) {
                            Text("⚙️", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlassSurfaceDeep,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.clickable { viewModel.exportChatAsMarkdown(context) }
                        ) {
                            Text("⬇️", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp))
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
                .padding(horizontal = 16.dp)
        ) {
            // 1. Gateway & Persona Status Ribbon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GlassSurfaceDeep,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(6.dp),
                            shape = CircleShape,
                            color = if (serverTelemetry.isRunning) Color(0xFF34D399) else Rose
                        ) {}

                        Text(
                            text = "Gateway :8080 · $enabledProfilesCount Provider Node${if (enabledProfilesCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val activeMcpTools = mcpServers.filter { it.isEnabled }.flatMap { it.discoveredTools }.count { it.isEnabled }
                        if (activeMcpTools > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Cyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "🛠️ $activeMcpTools MCP Tools",
                                    color = Cyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(activePersona.accentColorHex).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { showPersonaDialog = true }
                        ) {
                            Text(
                                text = "Temp: ${activeSession?.temperature ?: 0.7f}",
                                color = Color(activePersona.accentColorHex),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Clear",
                            color = Rose,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearCurrentChat() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Chat Messages Area
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IsometricCard(glowColor = Cyan) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(activePersona.icon, fontSize = 32.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "START CONVERSATION WITH ${activePersona.name.uppercase()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Cyan,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = activePersona.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Type your query below or pick a quick prompt from the carousel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. Quick Suggestions Carousel
            val quickPrompts = when (activePersona.id) {
                "code_architect" -> listOf(
                    "Write a Kotlin Flow with retry & timeout",
                    "Explain Jetpack Compose state hoisting",
                    "Optimize Android memory allocations",
                    "Design a clean repository pattern"
                )
                "cyber_operator" -> listOf(
                    "Explain AES-256 GCM authenticated encryption",
                    "Analyze Wi-Fi 802.11 security handshake",
                    "RFC 959 FTP protocol summary",
                    "mDNS Zeroconf discovery mechanisms"
                )
                "hardware_diagnostic" -> listOf(
                    "Analyze phone accelerometer gravity vector",
                    "Explain NDEF record payload formatting",
                    "Calculate Barometer pressure to altitude",
                    "Sensor sampling rates vs battery drain"
                )
                else -> listOf(
                    "Explain Quantum Computing in simple terms",
                    "Summarize recent advancements in AI models",
                    "Write a concise email to stakeholders",
                    "Help me brainstorm innovative app ideas"
                )
            }

            val promptScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(promptScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { q ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GlassSurfaceDeep,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.clickable {
                            viewModel.setInputPrompt(q)
                            viewModel.sendMessage()
                        }
                    ) {
                        Text(
                            text = "💡 $q",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4. Prompt Input Bar with Liquid Glass styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { viewModel.setInputPrompt(it) },
                    placeholder = { Text("Ask ${activePersona.name}...", fontSize = 12.sp) },
                    maxLines = 4,
                    colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                    modifier = Modifier.weight(1f)
                )

                LiquidGlassButton(
                    onClick = {
                        if (isGenerating) {
                            viewModel.stopGeneration()
                        } else {
                            viewModel.sendMessage()
                        }
                    },
                    enabled = isGenerating || inputPrompt.isNotBlank(),
                    glowColor = if (isGenerating) Rose else Cyan,
                    useRainbowBorder = !isGenerating,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isGenerating) "⏹ Stop" else "Send ➔",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGenerating) Rose else Cyan
                    )
                }
            }
        }
    }

    // Modal Drawers and Dialogs
    if (showSessionsDrawer) {
        ChatSessionDrawer(
            sessions = sessions,
            activeSessionId = activeSession?.id,
            onSelectSession = { viewModel.selectSession(it) },
            onNewSession = { viewModel.createNewSession(activePersona) },
            onDeleteSession = { viewModel.deleteSession(it) },
            onRenameSession = { id, name -> viewModel.renameSession(id, name) },
            onDismiss = { showSessionsDrawer = false }
        )
    }

    if (showPersonaDialog && activeSession != null) {
        PersonaConfigDialog(
            session = activeSession!!,
            onDismiss = { showPersonaDialog = false },
            onSave = { prompt, temp, model ->
                viewModel.updateSessionSettings(prompt, temp, model)
                showPersonaDialog = false
            }
        )
    }
}
