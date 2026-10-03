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

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(BorderStroke(1.dp, RainbowGlassBorderBrush), shape),
            color = MaterialTheme.colorScheme.surface,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "AI PERSONA & TUNING",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Customize system personality, model parameters, and target routes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(14.dp))

                // Persona Presets Grid
                Text(
                    text = "PRESET PERSONALITY:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersonaPreset.ALL_PRESETS.forEach { preset ->
                        val isSel = selectedPersonaId == preset.id
                        val presetAccent = Color(preset.accentColorHex)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) presetAccent.copy(alpha = 0.15f) else GlassSurfaceDeep,
                            border = BorderStroke(1.dp, if (isSel) presetAccent else GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPersonaId = preset.id
                                    systemPrompt = preset.systemPrompt
                                    temperature = preset.defaultTemperature
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(preset.icon, fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSel) presetAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // System Instructions Editor
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt (Instructions)", fontSize = 10.sp) },
                    maxLines = 4,
                    colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

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
                        inactiveTrackColor = GlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                // Target Model Override
                OutlinedTextField(
                    value = modelOverride,
                    onValueChange = { modelOverride = it },
                    label = { Text("Target Model Override (Leave blank for default)", fontSize = 10.sp) },
                    placeholder = { Text("e.g. gemini-1.5-flash, llama3.2:latest", fontSize = 9.sp) },
                    singleLine = true,
                    colors = liquidGlassTextFieldColors(focusedBorderColor = Cyan),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassButton(
                        onClick = onDismiss,
                        glowColor = TextSecondary,
                        text = "Cancel"
                    )

                    LiquidGlassButton(
                        onClick = {
                            onSave(systemPrompt, temperature, modelOverride.trim().ifBlank { null })
                        },
                        glowColor = Cyan,
                        useRainbowBorder = true,
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


