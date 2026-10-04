package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Acceleration Vector Sphere & Artificial Horizon for Accelerometer / Gravity / Linear Acceleration.
 * Displays a 3D physical force vector arrow + artificial horizon line + G-force magnitude readout.
 */
@Composable
fun SensorVectorSphere3D(
    rawValues: List<Float>?,
    unit: String = "m/s²",
    modifier: Modifier = Modifier
) {
    val rawX = rawValues?.getOrElse(0) { 0f } ?: 0f
    val rawY = rawValues?.getOrElse(1) { 0f } ?: 0f
    val rawZ = rawValues?.getOrElse(2) { 0f } ?: 9.81f

    val animX by animateFloatAsState(
        targetValue = rawX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "animX"
    )
    val animY by animateFloatAsState(
        targetValue = rawY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "animY"
    )
    val animZ by animateFloatAsState(
        targetValue = rawZ,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "animZ"
    )

    val magnitude = sqrt(animX * animX + animY * animY + animZ * animZ)
    val gForce = magnitude / 9.80665f

    // Pitch and Roll calculation relative to gravity (smoothed)
    val pitchDeg = Math.toDegrees(atan2(animY.toDouble(), sqrt((animX * animX + animZ * animZ).toDouble()))).toFloat()
    val rollDeg = Math.toDegrees(atan2(-animX.toDouble(), animZ.toDouble())).toFloat()

    val forceColor = when {
        gForce > 2.2f -> Rose
        gForce > 1.4f -> Color(0xFFF59E0B)
        gForce < 0.6f -> Violet
        else -> Emerald
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
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3D FORCE VECTOR & ATTITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = String.format(Locale.US, "%.2f G", gForce),
                    style = MaterialTheme.typography.labelSmall,
                    color = forceColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // 3D Sphere & Horizon Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = minOf(cx, cy) * 0.75f

                    // 1. Outer reference sphere ring
                    drawCircle(
                        color = Color(0xFF1B1E2B),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF282E40),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(1.2f)
                    )

                    // 2. Artificial Horizon Line (tilted by roll, shifted by pitch)
                    val rollRad = Math.toRadians(rollDeg.toDouble()).toFloat()
                    val pitchOffset = (pitchDeg / 90f) * (radius * 0.6f)
                    val horizonSlope = atan2(sin(rollRad), cos(rollRad))

                    val dx = cos(horizonSlope) * radius
                    val dy = sin(horizonSlope) * radius

                    // Sky / Ground split
                    val horizonP1 = Offset(cx - dx, cy - dy - pitchOffset)
                    val horizonP2 = Offset(cx + dx, cy + dy - pitchOffset)

                    drawLine(
                        color = Cyan.copy(alpha = 0.45f),
                        start = horizonP1,
                        end = horizonP2,
                        strokeWidth = 1.8f
                    )

                    // 3. 3D Isometric Coordinate Rings (X-ring, Y-ring, Z-ring)
                    // Equatorial ring (XY-plane in perspective)
                    drawOval(
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        topLeft = Offset(cx - radius * 0.85f, cy - radius * 0.35f),
                        size = Size(radius * 1.7f, radius * 0.7f),
                        style = Stroke(1f)
                    )
                    // Vertical ring (XZ-plane in perspective)
                    drawOval(
                        color = Color(0xFF34D399).copy(alpha = 0.2f),
                        topLeft = Offset(cx - radius * 0.35f, cy - radius * 0.85f),
                        size = Size(radius * 0.7f, radius * 1.7f),
                        style = Stroke(1f)
                    )

                    // 4. 3D Force Vector Arrow
                    // Vector length scaled to sphere radius
                    val maxNorm = 19.6f // ~2G max scale
                    val vectorLen = (magnitude / maxNorm).coerceIn(0.1f, 1f) * radius

                    // Vector angle on 2D projection (smoothed)
                    val vecAngle = atan2(-animY.toDouble(), animX.toDouble()).toFloat()
                    val tipX = cx + cos(vecAngle) * vectorLen
                    val tipY = cy + sin(vecAngle) * vectorLen

                    // Arrow shaft with neon glow
                    drawLine(
                        color = forceColor,
                        start = Offset(cx, cy),
                        end = Offset(tipX, tipY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Arrowhead
                    val headLen = 14f
                    val headAngle1 = vecAngle + Math.toRadians(150.0).toFloat()
                    val headAngle2 = vecAngle - Math.toRadians(150.0).toFloat()

                    val headPath = Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(tipX + cos(headAngle1) * headLen, tipY + sin(headAngle1) * headLen)
                        lineTo(tipX + cos(headAngle2) * headLen, tipY + sin(headAngle2) * headLen)
                        close()
                    }
                    drawPath(path = headPath, color = forceColor)

                    // Center pivot dot
                    drawCircle(
                        color = Color.White,
                        radius = 3.5f,
                        center = Offset(cx, cy)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Tilt & Magnitude Readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Roll" to String.format(Locale.US, "%+.1f°", rollDeg),
                    "Pitch" to String.format(Locale.US, "%+.1f°", pitchDeg),
                    "Magnitude" to String.format(Locale.US, "%.2f %s", magnitude, unit)
                ).forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141620),
                        border = BorderStroke(1.dp, Color(0xFF242735)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
