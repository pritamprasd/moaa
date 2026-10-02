package dev.pritam.dynamictools.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.ui.theme.Cyan
import dev.pritam.dynamictools.ui.theme.GlassBorder
import dev.pritam.dynamictools.ui.theme.Rose
import dev.pritam.dynamictools.ui.theme.SurfaceDeep
import dev.pritam.dynamictools.ui.theme.TextPrimary
import dev.pritam.dynamictools.ui.theme.TextSecondary
import dev.pritam.dynamictools.ui.theme.Violet

@Composable
fun DynamicToolRefineDialog(
    bundle: DynamicToolBundle,
    isRefining: Boolean,
    refinementStatus: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRefine: (String) -> Unit
) {
    var refinePrompt by remember { mutableStateOf("") }

    val presetSuggestions = listOf(
        "Add dark/light theme switch",
        "Add button sound/haptic feedback",
        "Add calculation history list",
        "Improve mobile contrast and button sizes",
        "Add one-tap copy to clipboard"
    )

    Dialog(onDismissRequest = { if (!isRefining) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDeep
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI ITERATION & REFINEMENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Violet,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Iterate '${bundle.manifest.displayName}' via LLM Gateway",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    if (!isRefining) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Rose, fontSize = 12.sp)
                        }
                    }
                }

                // Suggestion chips
                Text(
                    text = "QUICK SUGGESTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetSuggestions.forEach { suggestion ->
                        Surface(
                            onClick = { refinePrompt = suggestion },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2038BDF8),
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✨ $suggestion",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Prompt Input
                OutlinedTextField(
                    value = refinePrompt,
                    onValueChange = { refinePrompt = it },
                    placeholder = { Text("e.g., Add trigonometric power buttons and history tape...", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color(0xFF030712),
                        unfocusedContainerColor = Color(0xFF030712),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isRefining
                )

                if (errorMessage != null) {
                    Text(
                        text = "Error: $errorMessage",
                        color = Rose,
                        fontSize = 10.sp
                    )
                }

                if (isRefining) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Violet,
                            modifier = Modifier.padding(8.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = refinementStatus ?: "Generating updated code...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet,
                            fontSize = 11.sp
                        )
                    }
                }

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRefining
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = { onRefine(refinePrompt) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRefining && refinePrompt.isNotBlank()
                    ) {
                        Text("⚡ Refine with AI", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
