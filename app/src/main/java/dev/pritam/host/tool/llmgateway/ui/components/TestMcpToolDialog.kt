package dev.pritam.host.tool.llmgateway.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
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
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import org.json.JSONObject

@Composable
fun TestMcpToolDialog(
    tool: McpToolDefinition,
    isExecuting: Boolean,
    lastResult: McpToolResult?,
    onExecute: (argumentsJson: String) -> Unit,
    onDismiss: () -> Unit
) {
    var argumentsJson by remember {
        val defaultArgs = JSONObject()
        tool.inputSchema.properties.forEach { (k, v) ->
            when (v.type) {
                "string" -> defaultArgs.put(k, if (v.enumValues.isNotEmpty()) v.enumValues.first() else "")
                "integer", "number" -> defaultArgs.put(k, 10)
                "boolean" -> defaultArgs.put(k, true)
                else -> defaultArgs.put(k, JSONObject())
            }
        }
        mutableStateOf(if (defaultArgs.length() > 0) defaultArgs.toString(2) else "{}")
    }

    val shape = RoundedCornerShape(16.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = shape,
            color = Color(0xFF12141C),
            border = BorderStroke(1.dp, Color(0xFF222531)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(shape)
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Cyan
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "TEST MCP TOOL: ${tool.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp,
                            maxLines = 1
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
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "ARGUMENTS (JSON)",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = argumentsJson,
                    onValueChange = { argumentsJson = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Color(0xFF222531),
                        focusedContainerColor = Color(0xFF0C0E14),
                        unfocusedContainerColor = Color(0xFF0C0E14),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Output Result Box
                if (lastResult != null) {
                    Text(
                        text = if (lastResult.isError) "EXECUTION ERROR" else "EXECUTION RESULT (${lastResult.latencyMs}ms)",
                        color = if (lastResult.isError) Rose else Emerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))

                    val resultShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(resultShape)
                            .background(Color(0xFF0C0E14))
                            .border(BorderStroke(1.dp, if (lastResult.isError) Rose.copy(alpha = 0.5f) else Emerald.copy(alpha = 0.5f)), resultShape)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = lastResult.content,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = TextSecondary)
                    }

                    LiquidGlassButton(
                        onClick = { onExecute(argumentsJson) },
                        enabled = !isExecuting,
                        modifier = Modifier.weight(1f),
                        glowColor = Cyan,
                        useRainbowBorder = false,
                        text = if (isExecuting) "Running..." else "Run Tool Call"
                    )
                }
            }
        }
    }
}
