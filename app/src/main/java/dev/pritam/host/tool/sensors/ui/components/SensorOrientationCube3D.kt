package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Interactive 3D Perspective Projection Cube driven by spatial Rotation Vector telemetry.
 * Renders in real-time space with 6DOF Euler orientation (Roll, Pitch, Yaw).
 */
@Composable
fun SensorOrientationCube3D(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    // Rotation vector quaternion: [x, y, z, cos(θ/2)]
    val qx = rawValues?.getOrElse(0) { 0f } ?: 0f
    val qy = rawValues?.getOrElse(1) { 0f } ?: 0f
    val qz = rawValues?.getOrElse(2) { 0f } ?: 0f
    val qw = rawValues?.getOrElse(3) {
        val sumSq = qx * qx + qy * qy + qz * qz
        if (sumSq <= 1f) sqrt(1f - sumSq) else 1f
    } ?: 1f

    // Calculate Roll, Pitch, Yaw in degrees from quaternion
    val sinrCosp = 2f * (qw * qx + qy * qz)
    val cosrCosp = 1f - 2f * (qx * qx + qy * qy)
    val rollDeg = Math.toDegrees(atan2(sinrCosp.toDouble(), cosrCosp.toDouble())).toFloat()

    val sinp = 2f * (qw * qy - qz * qx)
    val pitchDeg = if (kotlin.math.abs(sinp) >= 1f) {
        Math.copySign(90.0, sinp.toDouble()).toFloat()
    } else {
        Math.toDegrees(kotlin.math.asin(sinp.toDouble())).toFloat()
    }

    val sinyCosp = 2f * (qw * qz + qx * qy)
    val cosyCosp = 1f - 2f * (qy * qy + qz * qz)
    val yawDeg = Math.toDegrees(atan2(sinyCosp.toDouble(), cosyCosp.toDouble())).toFloat()

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
                    text = "3D DEVICE ORIENTATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "6DOF PERSPECTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(4.dp))

            // 3D Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val cubeSize = minOf(cx, cy) * 0.72f
                    val f = 280f // Focal distance for perspective

                    // 8 Vertices of a cube centered at origin (-1 to +1)
                    val baseVertices = listOf(
                        floatArrayOf(-1f, -1f, -1f),
                        floatArrayOf(1f, -1f, -1f),
                        floatArrayOf(1f, 1f, -1f),
                        floatArrayOf(-1f, 1f, -1f),
                        floatArrayOf(-1f, -1f, 1f),
                        floatArrayOf(1f, -1f, 1f),
                        floatArrayOf(1f, 1f, 1f),
                        floatArrayOf(-1f, 1f, 1f)
                    )

                    // Convert Euler angles to radians
                    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
                    val rollRad = Math.toRadians(rollDeg.toDouble()).toFloat()
                    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()

                    val cosP = cos(pitchRad)
                    val sinP = sin(pitchRad)
                    val cosR = cos(rollRad)
                    val sinR = sin(rollRad)
                    val cosY = cos(yawRad)
                    val sinY = sin(yawRad)

                    // Rotate & project vertices
                    val projected = baseVertices.map { v ->
                        val x0 = v[0] * cubeSize
                        val y0 = v[1] * cubeSize
                        val z0 = v[2] * cubeSize

                        // Rotation around X (Pitch)
                        val y1 = y0 * cosP - z0 * sinP
                        val z1 = y0 * sinP + z0 * cosP
                        val x1 = x0

                        // Rotation around Y (Roll)
                        val x2 = x1 * cosR + z1 * sinR
                        val z2 = -x1 * sinR + z1 * cosR
                        val y2 = y1

                        // Rotation around Z (Yaw)
                        val x3 = x2 * cosY - y2 * sinY
                        val y3 = x2 * sinY + y2 * cosY
                        val z3 = z2

                        // Perspective projection
                        val projZ = z3 + 300f
                        val factor = f / projZ
                        val px = cx + x3 * factor
                        val py = cy - y3 * factor // Invert Y for screen coordinates
                        Triple(px, py, z3)
                    }

                    // 12 Edges connecting vertices
                    val edges = listOf(
                        0 to 1, 1 to 2, 2 to 3, 3 to 0, // Back face
                        4 to 5, 5 to 6, 6 to 7, 7 to 4, // Front face
                        0 to 4, 1 to 5, 2 to 6, 3 to 7  // Connecting edges
                    )

                    // Draw 3D axis coordinate crosshair in the background
                    drawLine(
                        color = Color(0xFF1E2433),
                        start = Offset(cx - 70f, cy),
                        end = Offset(cx + 70f, cy),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF1E2433),
                        start = Offset(cx, cy - 70f),
                        end = Offset(cx, cy + 70f),
                        strokeWidth = 1f
                    )

                    // Top face fill indicator (Vertices 4, 5, 1, 0)
                    val topFacePath = Path().apply {
                        moveTo(projected[4].first, projected[4].second)
                        lineTo(projected[5].first, projected[5].second)
                        lineTo(projected[1].first, projected[1].second)
                        lineTo(projected[0].first, projected[0].second)
                        close()
                    }
                    drawPath(
                        path = topFacePath,
                        color = Cyan.copy(alpha = 0.12f)
                    )

                    // Draw wireframe edges
                    edges.forEach { (i, j) ->
                        val p1 = projected[i]
                        val p2 = projected[j]
                        val avgZ = (p1.third + p2.third) / 2f
                        val alpha = ((avgZ + cubeSize) / (2f * cubeSize)).coerceIn(0.25f, 1f)

                        drawLine(
                            color = Cyan.copy(alpha = alpha),
                            start = Offset(p1.first, p1.second),
                            end = Offset(p2.first, p2.second),
                            strokeWidth = if (avgZ > 0) 2.2f else 1.2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw glowing vertices
                    projected.forEach { (px, py, pz) ->
                        val pointAlpha = ((pz + cubeSize) / (2f * cubeSize)).coerceIn(0.3f, 1f)
                        drawCircle(
                            color = Emerald.copy(alpha = pointAlpha),
                            radius = if (pz > 0) 3.5f else 2.5f,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Euler Angle Badges (Roll, Pitch, Yaw)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Roll" to rollDeg,
                    "Pitch" to pitchDeg,
                    "Yaw" to yawDeg
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
                                text = String.format(Locale.US, "%+.1f°", value),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (value >= 0) Cyan else Emerald,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
