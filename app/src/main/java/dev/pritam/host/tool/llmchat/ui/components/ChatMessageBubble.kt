package dev.pritam.host.tool.llmchat.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ftp.ui.components.LiquidGlassButton
import dev.pritam.host.ftp.ui.components.RainbowGlassBorderBrush
import dev.pritam.host.tool.llmchat.model.ChatMessage
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isUser = message.role == "user"
    val timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    }

    val bubbleBorder = if (isUser) {
        Brush.linearGradient(listOf(Cyan.copy(alpha = 0.7f), GlassBorder))
    } else if (message.isError) {
        Brush.linearGradient(listOf(Rose, Rose.copy(alpha = 0.5f)))
    } else {
        RainbowGlassBorderBrush
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)) {
            // Clean depth shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 2.dp, start = if (isUser) 2.dp else 0.dp, end = if (isUser) 0.dp else 2.dp)
                    .clip(bubbleShape)
                    .background(Color(0x35030712))
            )

            // Frosted Acrylic Glass Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bubbleShape)
                    .border(BorderStroke(1.dp, bubbleBorder), bubbleShape),
                color = if (isUser) Color(0x351E293B) else GlassSurfaceElevated,
                shape = bubbleShape
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Bar: Role Tag + Model Badge / Timestamp + Copy Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isUser) Cyan.copy(alpha = 0.2f) else if (message.isError) Rose.copy(alpha = 0.2f) else Color(0xFF34D399).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isUser) "YOU" else if (message.isError) "ERROR" else "ASSISTANT",
                                    color = if (isUser) Cyan else if (message.isError) Rose else Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            if (!isUser && message.modelUsed != null) {
                                Text(
                                    text = "· ${message.modelUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            LiquidGlassButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Chat Message", message.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                glowColor = TextSecondary,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Copy", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // MCP Tool Calls & Execution Badges
                    if (message.toolCalls.isNotEmpty() || message.toolResults.isNotEmpty()) {
                        McpToolCallsAccordion(
                            toolCalls = message.toolCalls,
                            toolResults = message.toolResults
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Message Body with Code Block Formatting
                    if (message.content.isBlank() && message.isStreaming) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = Color(0xFF34D399)) {}
                            Text("Thinking & routing via Gateway...", color = Color(0xFF34D399), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    } else {
                        FormattedContent(content = message.content, context = context)
                    }

                    // Failover Trail Badge (if failover occurred)
                    if (message.failoverTrail.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Violet.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Violet.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ Failover recovered: ${message.failoverTrail.joinToString(" ➔ ")} ➔ [${message.providerUsed ?: "Final"}]",
                                color = Violet,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Latency / Tokens telemetry
                    if (!isUser && message.latencyMs > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Latency: ${message.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Splits message text into regular paragraphs and code blocks ```code```.
 */
@Composable
private fun FormattedContent(content: String, context: Context) {
    val parts = content.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val lines = part.trim().lines()
                val lang = if (lines.isNotEmpty() && lines.first().length < 15 && !lines.first().contains(" ")) lines.first() else "code"
                val codeBody = if (lang != "code" && lines.size > 1) lines.drop(1).joinToString("\n") else part.trim()

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF090E17),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.uppercase(),
                                color = Cyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            LiquidGlassButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Code Block", codeBody)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                },
                                glowColor = Cyan,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("📋 Copy Code", fontSize = 8.sp, color = Cyan)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = codeBody,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                // Plain text / Markdown
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun McpToolCallsAccordion(
    toolCalls: List<dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall>,
    toolResults: List<dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Surface(
        shape = shape,
        color = Color(0x280284C7),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛠️", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "MCP Tools (${toolCalls.size.coerceAtLeast(toolResults.size)}): ${toolCalls.joinToString { it.name }.ifBlank { toolResults.joinToString { it.toolName } }}",
                        color = Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Text(
                    text = if (isExpanded) "Hide ▲" else "View ▼",
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    toolCalls.forEach { call ->
                        val matchingResult = toolResults.firstOrNull { it.callId == call.id || it.toolName == call.name }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassSurfaceDeep)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text(
                                    text = "▶ Tool Call: ${call.name}",
                                    color = Cyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                if (call.argumentsJson.isNotBlank() && call.argumentsJson != "{}") {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Args: ${call.argumentsJson}",
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (matchingResult != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Result Output (${matchingResult.latencyMs}ms):",
                                        color = if (matchingResult.isError) Rose else Color(0xFF34D399),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = matchingResult.content.take(300) + if (matchingResult.content.length > 300) "..." else "",
                                        color = TextPrimary,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun ChatMessageBubbleUserPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        ChatMessageBubble(
            message = ChatMessage(
                id = "1",
                role = "user",
                content = "Can you scan the nearby BLE and FTP devices on the local network?",
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF080E1A)
@Composable
private fun ChatMessageBubbleAssistantPreview() {
    dev.pritam.host.ui.theme.AppTheme {
        ChatMessageBubble(
            message = ChatMessage(
                id = "2",
                role = "assistant",
                content = "Running scan across subnet `192.168.1.0/24`:\n- Found **FTP Server** on port 2121\n- Active telemetry: 12.4 MB/s transfer speed.",
                timestamp = System.currentTimeMillis(),
                modelUsed = "gemini-2.5-flash"
            )
        )
    }
}



