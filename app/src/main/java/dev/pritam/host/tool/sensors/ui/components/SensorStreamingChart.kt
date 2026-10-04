package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class HistoryWindow(val label: String, val durationMs: Long) {
    LAST_30_SEC("30s", 30_000L),
    LAST_1_MIN("1m", 60_000L)
}

private val CHANNEL_COLORS = listOf(
    Cyan,               // Channel 0 (X / Primary)
    Emerald,            // Channel 1 (Y / Secondary)
    Violet,             // Channel 2 (Z / Tertiary)
    Color(0xFFF59E0B)   // Channel 3 (W / 4th axis)
)

private val CHANNEL_LABELS = listOf("X", "Y", "Z", "W")

@Composable
fun SensorStreamingChart(
    historyBuffer: SensorHistoryBuffer,
    unit: String,
    modifier: Modifier = Modifier
) {
    var selectedWindow by remember { mutableStateOf(HistoryWindow.LAST_30_SEC) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Header Row: Channel Legend + 30s/1m Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "LIVE TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    val numChannels = historyBuffer.points.lastOrNull()?.values?.size ?: 0
                    if (numChannels > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0 until min(numChannels, 4)).forEach { chIdx ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CHANNEL_COLORS[chIdx])
                                    )
                                    Text(
                                        text = CHANNEL_LABELS[chIdx],
                                        color = CHANNEL_COLORS[chIdx],
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 30s / 1m Time Window Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF14161F))
                        .border(1.dp, Color(0xFF242735), RoundedCornerShape(4.dp))
                        .padding(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    HistoryWindow.entries.forEach { win ->
                        val isSelected = selectedWindow == win
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) Cyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedWindow = win }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = win.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Cyan else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Waveform Plot Canvas
            val allPoints = historyBuffer.points
            val now = System.currentTimeMillis()
            val windowCutoff = now - selectedWindow.durationMs

            // Filter points within selected window
            val visiblePoints = remember(allPoints.size, selectedWindow) {
                allPoints.filter { it.timestampMs >= windowCutoff }
            }

            if (visiblePoints.size < 2) {
                // Buffering placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color(0xFF0D0F16), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1A1C26), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Buffering ${selectedWindow.label} sensor stream...",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Compute dynamic min/max across all visible channels
                var minVal = Float.MAX_VALUE
                var maxVal = -Float.MAX_VALUE
                visiblePoints.forEach { pt ->
                    pt.values.forEach { v ->
                        if (v < minVal) minVal = v
                        if (v > maxVal) maxVal = v
                    }
                }

                // Give 10% breathing room on Y-axis
                val paddingY = max((maxVal - minVal) * 0.12f, 0.05f)
                val yMin = minVal - paddingY
                val yMax = maxVal + paddingY
                val yRange = max(yMax - yMin, 0.001f)

                val numChannels = visiblePoints.first().values.size

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0C13), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF181B26), RoundedCornerShape(6.dp))
                    ) {
                        val w = size.width
                        val h = size.height

                        // Zero reference line if 0 is inside the range
                        if (0f in yMin..yMax) {
                            val zeroY = h - ((0f - yMin) / yRange) * h
                            drawLine(
                                color = Color(0xFF262A38),
                                start = Offset(0f, zeroY),
                                end = Offset(w, zeroY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }

                        // Background grid division lines (top, middle, bottom)
                        drawLine(
                            color = Color(0xFF161924),
                            start = Offset(0f, h * 0.25f),
                            end = Offset(w, h * 0.25f),
                            strokeWidth = 0.8f
                        )
                        drawLine(
                            color = Color(0xFF161924),
                            start = Offset(0f, h * 0.75f),
                            end = Offset(w, h * 0.75f),
                            strokeWidth = 0.8f
                        )

                        val timeStart = windowCutoff
                        val timeSpan = selectedWindow.durationMs.toFloat()

                        // Draw each channel's waveform curve
                        for (chIdx in 0 until min(numChannels, 4)) {
                            val color = CHANNEL_COLORS[chIdx]
                            val strokePath = Path()
                            val fillPath = Path()

                            var first = true
                            var startX = 0f
                            var lastX = 0f

                            visiblePoints.forEach { pt ->
                                val channelValue = pt.values.getOrNull(chIdx) ?: return@forEach
                                val xRatio = (pt.timestampMs - timeStart) / timeSpan
                                val x = xRatio.coerceIn(0f, 1f) * w
                                val yRatio = (channelValue - yMin) / yRange
                                val y = (h - yRatio.coerceIn(0f, 1f) * h)

                                if (first) {
                                    strokePath.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                    startX = x
                                    first = false
                                } else {
                                    strokePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                                lastX = x
                            }

                            if (!first) {
                                fillPath.lineTo(lastX, h)
                                fillPath.close()

                                // Soft ambient gradient under the primary channel
                                if (chIdx == 0) {
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
                                            startY = 0f,
                                            endY = h
                                        )
                                    )
                                }

                                // Waveform stroke
                                drawPath(
                                    path = strokePath,
                                    color = color,
                                    style = Stroke(
                                        width = if (numChannels == 1) 2.2f else 1.6f,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    // Y-Axis Min / Max Labels overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "%+.2f %s", maxVal, unit),
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%+.2f %s", minVal, unit),
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
