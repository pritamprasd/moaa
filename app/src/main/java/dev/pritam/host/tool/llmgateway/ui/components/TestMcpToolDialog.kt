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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚙️ Test MCP Tool: ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = tool.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "ARGUMENTS (JSON)",
                    color = Cyan,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = GlassBorder,
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurfaceDeep)
                            .border(BorderStroke(1.dp, if (lastResult.isError) Rose.copy(alpha = 0.4f) else Emerald.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = lastResult.content,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { onExecute(argumentsJson) },
                        enabled = !isExecuting,
                        border = BorderStroke(1.dp, Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Cyan.copy(alpha = 0.15f),
                            contentColor = Cyan
                        )
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(color = Cyan, modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Running...", fontSize = 12.sp)
                        } else {
                            Text("▶ Run Tool Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
