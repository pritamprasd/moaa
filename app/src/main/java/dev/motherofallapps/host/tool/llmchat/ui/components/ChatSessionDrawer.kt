package dev.motherofallapps.host.tool.llmchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.motherofallapps.host.ftp.ui.components.LiquidGlassButton
import dev.motherofallapps.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.motherofallapps.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.motherofallapps.host.tool.llmchat.model.ChatSession
import dev.motherofallapps.host.tool.llmchat.model.PersonaPreset
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.GlassBorder
import dev.motherofallapps.host.ui.theme.GlassSurfaceDeep
import dev.motherofallapps.host.ui.theme.GlassSurfaceElevated
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatSessionDrawer(
    sessions: List<ChatSession>,
    activeSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(BorderStroke(1.dp, RainbowGlassBorderBrush), shape),
            color = GlassSurfaceElevated,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONVERSATION THREADS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan,
                        letterSpacing = 1.sp
                    )

                    LiquidGlassButton(
                        onClick = {
                            onNewSession()
                            onDismiss()
                        },
                        glowColor = Cyan,
                        useRainbowBorder = true,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        text = "+ New Chat"
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (sessions.isEmpty()) {
                    Text("No conversations yet", color = TextSecondary, fontSize = 11.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val isSelected = session.id == activeSessionId
                            val preset = PersonaPreset.findById(session.personaId)
                            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.updatedAt))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Cyan.copy(alpha = 0.15f) else GlassSurfaceDeep,
                                border = BorderStroke(1.dp, if (isSelected) Cyan else GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSession(session.id)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(preset.icon, fontSize = 16.sp)
                                        Column {
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Cyan else TextPrimary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${session.messages.size} msgs · $dateStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LiquidGlassButton(
                                            onClick = {
                                                sessionToRename = session
                                                renameText = session.title
                                            },
                                            glowColor = TextSecondary,
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            text = "✏️"
                                        )

                                        if (sessions.size > 1) {
                                            LiquidGlassButton(
                                                onClick = { onDeleteSession(session.id) },
                                                glowColor = Rose,
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                text = "🗑"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                LiquidGlassButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = TextSecondary,
                    text = "Close"
                )
            }
        }
    }

    if (sessionToRename != null) {
        Dialog(onDismissRequest = { sessionToRename = null }) {
            val shape = RoundedCornerShape(12.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(BorderStroke(1.dp, Cyan), shape),
                color = GlassSurfaceElevated,
                shape = shape
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rename Conversation", fontWeight = FontWeight.Bold, color = Cyan, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        LiquidGlassButton(
                            onClick = { sessionToRename = null },
                            glowColor = TextSecondary,
                            text = "Cancel"
                        )
                        LiquidGlassButton(
                            onClick = {
                                sessionToRename?.let { s ->
                                    if (renameText.isNotBlank()) {
                                        onRenameSession(s.id, renameText.trim())
                                    }
                                }
                                sessionToRename = null
                            },
                            glowColor = Cyan,
                            text = "Save"
                        )
                    }
                }
            }
        }
    }
}
