package dev.pritam.host.tool.llmgateway.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.ftp.ui.components.liquidGlassTextFieldColors
import dev.pritam.host.tool.llmgateway.model.LlmProfile
import dev.pritam.host.tool.llmgateway.model.ProfileStatus
import dev.pritam.host.tool.llmgateway.model.ProviderCategory
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet
import java.util.UUID

@Composable
fun AddCloudProfileDialog(
    onDismiss: () -> Unit,
    onSave: (LlmProfile) -> Unit
) {
    var selectedProvider by remember { mutableStateOf("GEMINI_CLOUD") }
    var name by remember { mutableStateOf("Google Gemini Flash") }
    var email by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var targetModel by remember { mutableStateOf("gemini-1.5-flash") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Column(modifier = Modifier.padding(20.dp)) {
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
                            text = "ADD CLOUD LLM ACCOUNT",
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
                    text = "Connect Google Gemini or OpenAI ChatGPT cloud services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(14.dp))

                // Provider Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providers = listOf(
                        "GEMINI_CLOUD" to "Google Gemini",
                        "CHATGPT_CLOUD" to "OpenAI ChatGPT"
                    )
                    providers.forEach { (type, label) ->
                        val isSel = selectedProvider == type
                        LiquidGlassButton(
                            onClick = {
                                selectedProvider = type
                                if (type == "GEMINI_CLOUD") {
                                    name = "Google Gemini Flash"
                                    targetModel = "gemini-1.5-flash"
                                } else {
                                    name = "ChatGPT 4o-mini"
                                    targetModel = "gpt-4o-mini"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            glowColor = if (isSel) Cyan else TextSecondary,
                            useRainbowBorder = false,
                            text = label
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (selectedProvider == "GEMINI_CLOUD") "Gemini API Key (AI Studio)" else "OpenAI API Key (sk-...)", fontSize = 11.sp) },
                    placeholder = { Text("Leave blank if using OAuth / SSO", fontSize = 10.sp, color = Color(0xFF6B7280)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Account Email (Optional)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetModel,
                    onValueChange = { targetModel = it },
                    label = { Text("Target Model Identifier", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage!!, color = Rose, fontSize = 11.sp)
                }

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
                            if (name.isBlank()) {
                                errorMessage = "Profile name cannot be blank"
                                return@LiquidGlassButton
                            }
                            val profile = LlmProfile(
                                id = "profile-${UUID.randomUUID()}",
                                name = name.trim(),
                                category = ProviderCategory.CLOUD_OAUTH,
                                providerType = selectedProvider,
                                accountEmail = email.trim().ifBlank { null },
                                apiKey = apiKey.trim().ifBlank { null },
                                targetModel = targetModel.trim().ifBlank { "gemini-1.5-flash" },
                                status = ProfileStatus.IDLE,
                                isEnabled = true
                            )
                            onSave(profile)
                        },
                        modifier = Modifier.weight(1f),
                        glowColor = Cyan,
                        useRainbowBorder = false,
                        text = "Save Profile"
                    )
                }
            }
        }
    }
}

@Composable
fun AddDesktopHostDialog(
    onDismiss: () -> Unit,
    onSave: (LlmProfile) -> Unit
) {
    var name by remember { mutableStateOf("Desktop Ollama") }
    var hostAddress by remember { mutableStateOf("http://192.168.1.100:11434") }
    var targetModel by remember { mutableStateOf("llama3.2:latest") }
    var serviceType by remember { mutableStateOf("OLLAMA_LOCAL") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Column(modifier = Modifier.padding(20.dp)) {
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
                            color = Color(0xFF34D399) // Emerald
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "ADD DESKTOP LOCAL HOST",
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
                    text = "Direct HTTP integration with LAN LLM servers (Ollama, LM Studio, vLLM). No credentials or API keys needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(14.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "OLLAMA_LOCAL" to ("Ollama" to "http://192.168.1.100:11434"),
                        "LM_STUDIO" to ("LM Studio" to "http://192.168.1.100:1234"),
                        "CUSTOM_OPENAI" to ("vLLM / Other" to "http://192.168.1.100:8000")
                    )
                    presets.forEach { (type, meta) ->
                        val isSel = serviceType == type
                        LiquidGlassButton(
                            onClick = {
                                serviceType = type
                                hostAddress = meta.second
                                name = "Desktop ${meta.first}"
                            },
                            modifier = Modifier.weight(1f),
                            glowColor = if (isSel) Color(0xFF34D399) else TextSecondary,
                            useRainbowBorder = false,
                            text = meta.first
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = hostAddress,
                    onValueChange = { hostAddress = it },
                    label = { Text("Host URL & Port (e.g. http://192.168.1.50:11434)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetModel,
                    onValueChange = { targetModel = it },
                    label = { Text("Default Model Tag (e.g. llama3.2:latest)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = liquidGlassTextFieldColors(
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF222531),
                        containerColor = Color(0xFF0C0E14)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage!!, color = Rose, fontSize = 11.sp)
                }

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
                            if (name.isBlank()) {
                                errorMessage = "Profile name cannot be blank"
                                return@LiquidGlassButton
                            }
                            if (hostAddress.isBlank()) {
                                errorMessage = "Host address cannot be blank"
                                return@LiquidGlassButton
                            }
                            val profile = LlmProfile(
                                id = "profile-${UUID.randomUUID()}",
                                name = name.trim(),
                                category = ProviderCategory.DESKTOP_LOCAL_HOST,
                                providerType = serviceType,
                                hostAddress = hostAddress.trim(),
                                targetModel = targetModel.trim().ifBlank { "llama3.2:latest" },
                                status = ProfileStatus.IDLE,
                                isEnabled = true
                            )
                            onSave(profile)
                        },
                        modifier = Modifier.weight(1f),
                        glowColor = Color(0xFF34D399),
                        useRainbowBorder = false,
                        text = "Save Desktop Host"
                    )
                }
            }
        }
    }
}

