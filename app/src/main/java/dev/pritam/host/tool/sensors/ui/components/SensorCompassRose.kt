package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import dev.pritam.host.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tactical Compass Rose visualizer for Geomagnetic Field sensors.
 * Displays rotating azimuth dial, North/South magnetic needles, and flux intensity.
 */
@Composable
fun SensorCompassRose(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val x = rawValues?.getOrNull(0) ?: 0f
    val y = rawValues?.getOrNull(1) ?: 0f
    val z = rawValues?.getOrNull(2) ?: 0f

    // Azimuth in degrees (0..360) where 0 is North
    val headingDeg = remember(x, y) {
        val rad = atan2(-x.toDouble(), y.toDouble())
        var deg = Math.toDegrees(rad).toFloat()
        if (deg < 0) deg += 360f
        deg
    }

    // Continuous unwrapped heading to avoid 360°/0° spin glitches during spring interpolation
    var continuousHeading by remember { mutableFloatStateOf(headingDeg) }
    LaunchedEffect(headingDeg) {
        val diff = (headingDeg - continuousHeading + 180f) % 360f - 180f
        val shortestDiff = if (diff < -180f) diff + 360f else diff
        continuousHeading += shortestDiff
    }

    val animHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "animHeading"
    )

    // Magnitude of magnetic flux in microteslas (µT)
    val magnitude = remember(x, y, z) {
        sqrt(x * x + y * y + z * z)
    }

    val cardinalDirection = remember(headingDeg) {
        when {
            headingDeg >= 337.5 || headingDeg < 22.5 -> "N"
            headingDeg in 22.5..67.5 -> "NE"
            headingDeg in 67.5..112.5 -> "E"
            headingDeg in 112.5..157.5 -> "SE"
            headingDeg in 157.5..202.5 -> "S"
            headingDeg in 202.5..247.5 -> "SW"
            headingDeg in 247.5..292.5 -> "W"
            else -> "NW"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAGNETIC COMPASS HUD",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "TOTAL FLUX: %.1f µT", magnitude),
                    color = Cyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compass Dial Canvas
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(170.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 8.dp.toPx()

                    // Outer bezel ring
                    drawCircle(
                        color = GlassBorder,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Inner radar sweep ring
                    drawCircle(
                        color = Cyan.copy(alpha = 0.08f),
                        radius = radius * 0.65f,
                        center = center
                    )
                    drawCircle(
                        color = GlassBorder.copy(alpha = 0.5f),
                        radius = radius * 0.65f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Tick marks around dial (rotate smoothly relative to heading)
                    rotate(-animHeading, pivot = center) {
                        for (angle in 0 until 360 step 15) {
                            val isMajor = angle % 90 == 0
                            val isMedium = angle % 30 == 0
                            val tickLen = when {
                                isMajor -> 10.dp.toPx()
                                isMedium -> 6.dp.toPx()
                                else -> 3.dp.toPx()
                            }
                            val tickColor = when {
                                angle == 0 -> Rose
                                isMajor -> TextPrimary
                                isMedium -> TextSecondary
                                else -> TextTertiary.copy(alpha = 0.4f)
                            }
                            val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                            val rad = Math.toRadians(angle.toDouble()).toFloat()
                            val cosA = cos(rad)
                            val sinA = sin(rad)

                            val startX = center.x + (radius - tickLen) * sinA
                            val startY = center.y - (radius - tickLen) * cosA
                            val endX = center.x + radius * sinA
                            val endY = center.y - radius * cosA

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }

                        // Compass Needle (North = Rose, South = Cyan)
                        val needleHalfW = 6.dp.toPx()
                        val needleLen = radius * 0.6f

                        // North Pointer
                        val northPath = Path().apply {
                            moveTo(center.x, center.y - needleLen)
                            lineTo(center.x + needleHalfW, center.y)
                            lineTo(center.x - needleHalfW, center.y)
                            close()
                        }
                        drawPath(northPath, brush = Brush.verticalGradient(listOf(Rose, Rose.copy(alpha = 0.7f))))

                        // South Pointer
                        val southPath = Path().apply {
                            moveTo(center.x, center.y + needleLen)
                            lineTo(center.x + needleHalfW, center.y)
                            lineTo(center.x - needleHalfW, center.y)
                            close()
                        }
                        drawPath(southPath, brush = Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.5f), Cyan)))
                    }

                    // Center pivot pin
                    drawCircle(
                        color = TextPrimary,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = GlassSurfaceDeep,
                        radius = 2.dp.toPx(),
                        center = center
                    )

                    // Top indicator arrow (fixed forward bearing)
                    val forwardArrow = Path().apply {
                        val tipY = center.y - radius - 4.dp.toPx()
                        moveTo(center.x, tipY)
                        lineTo(center.x - 5.dp.toPx(), tipY - 6.dp.toPx())
                        lineTo(center.x + 5.dp.toPx(), tipY - 6.dp.toPx())
                        close()
                    }
                    drawPath(forwardArrow, color = Rose)
                }

                // Center numeric readout overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%03.0f° %s", headingDeg, cardinalDirection),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Axis breakdown chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AxisChip(label = "X", value = x, color = Cyan, modifier = Modifier.weight(1f))
                AxisChip(label = "Y", value = y, color = Emerald, modifier = Modifier.weight(1f))
                AxisChip(label = "Z", value = z, color = Rose, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AxisChip(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                text = String.format(Locale.US, "%.1f", value),
                color = TextPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
