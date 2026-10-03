package dev.pritam.host.tool.llmchat.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.tool.llmchat.model.ChatSession
import dev.pritam.host.tool.llmchat.model.PersonaPreset
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@Composable
fun PersonaConfigDialog(
    session: ChatSession,
    onDismiss: () -> Unit,
    onSave: (systemPrompt: String, temperature: Float, targetModelOverride: String?) -> Unit,
) {
    var selectedPersonaId by remember { mutableStateOf(session.personaId) }
    var systemPrompt by remember { mutableStateOf(session.systemPrompt) }
    var temperature by remember { mutableFloatStateOf(session.temperature) }
    var modelOverride by remember { mutableStateOf(session.targetModelOverride ?: "") }

    val shape = RoundedCornerShape(16.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(shape)
                .border(BorderStroke(1.dp, Color(0xFF222531)), shape),
            color = Color(0xFF12141C),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Cyan
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AI PERSONA & TUNING",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        )
                    }
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

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Customize system personality, model parameters, and target routes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(16.dp))

                // Persona Presets Grid
                Text(
                    text = "PRESET PERSONALITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersonaPreset.ALL_PRESETS.forEach { preset ->
                        val isSel = selectedPersonaId == preset.id
                        val presetAccent = Color(preset.accentColorHex)
                        val itemShape = RoundedCornerShape(10.dp)

                        Surface(
                            shape = itemShape,
                            color = if (isSel) presetAccent.copy(alpha = 0.15f) else Color(0xFF0C0E14),
                            border = BorderStroke(1.dp, if (isSel) presetAccent else Color(0xFF222531)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .clickable {
                                    selectedPersonaId = preset.id
                                    systemPrompt = preset.systemPrompt
                                    temperature = preset.defaultTemperature
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(preset.icon, fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSel) presetAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // System Instructions Editor
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt (Instructions)", fontSize = 11.sp) },
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temperature (Creativity):", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f", temperature), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Cyan,
                        activeTrackColor = Cyan,
                        inactiveTrackColor = Color(0xFF222531)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Target Model Override
                OutlinedTextField(
                    value = modelOverride,
                    onValueChange = { modelOverride = it },
                    label = { Text("Target Model Override (Leave blank for default)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. gemini-1.5-flash, llama3.2:latest", fontSize = 10.sp, color = Color(0xFF6B7280)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    LiquidGlassButton(
                        onClick = {
                            onSave(systemPrompt, temperature, modelOverride.trim().ifBlank { null })
                        },
                        modifier = Modifier.weight(1f),
                        glowColor = Cyan,
                        useRainbowBorder = false,
                        text = "Apply Settings"
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun PersonaConfigDialogPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        PersonaConfigDialog(
            session = dev.pritam.host.tool.llmchat.model.ChatSession(
                title = "Ops Specialist",
                systemPrompt = "You are a cybernetic network operations specialist.",
                temperature = 0.7f,
                targetModelOverride = "gemini-2.5-flash"
            ),
            onDismiss = {},
            onSave = { _, _, _ -> }
        )
    }
}


