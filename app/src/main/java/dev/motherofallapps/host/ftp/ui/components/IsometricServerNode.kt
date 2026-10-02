package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.CyanDim
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.Violet
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Isometric Server Node Canvas with animated glowing grid, server bays, and LAN telemetry beams.
 */
@Composable
fun IsometricServerNode(
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "isometric_anim")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packets"
    )

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 10.dp.toPx()

            // 1. Draw Isometric Ground Grid
            drawIsometricGrid(cx, cy + 30.dp.toPx(), isRunning, pulse)

            // 2. Draw Isometric Server Stacks (3 stacked blade servers)
            val stackHeight = 24.dp.toPx()
            val baseWidth = 70.dp.toPx()
            val baseDepth = 40.dp.toPx()

            for (i in 0..2) {
                val offsetY = cy - (i * (stackHeight + 6.dp.toPx()))
                drawIsometricServerUnit(
                    centerX = cx,
                    centerY = offsetY,
                    width = baseWidth,
                    depth = baseDepth,
                    height = stackHeight,
                    layerIndex = i,
                    isRunning = isRunning,
                    pulse = pulse
                )
            }

            // 3. Draw Isometric Data Beams & Packets when running
            if (isRunning) {
                drawDataPackets(cx, cy - 60.dp.toPx(), packetProgress)
            }
        }
    }
}

private fun DrawScope.drawIsometricGrid(
    centerX: Float,
    centerY: Float,
    isRunning: Boolean,
    pulse: Float
) {
    val gridColor = if (isRunning) {
        CyanDim.copy(alpha = 0.35f * pulse)
    } else {
        Color(0xFF334155).copy(alpha = 0.2f)
    }

    val radius = 90.dp.toPx()
    val angle = 30f * (Math.PI.toFloat() / 180f)
    val dx = cos(angle) * radius
    val dy = sin(angle) * radius

    // Outer Isometric Diamond Ring
    val gridPath = Path().apply {
        moveTo(centerX, centerY - dy)
        lineTo(centerX + dx, centerY)
        lineTo(centerX, centerY + dy)
        lineTo(centerX - dx, centerY)
        close()
    }

    drawPath(gridPath, color = gridColor, style = Stroke(width = 1.5.dp.toPx()))

    // Inner perspective grid lines
    for (i in 1..3) {
        val fraction = i / 4f
        val fDx = dx * fraction
        val fDy = dy * fraction

        drawLine(
            color = gridColor.copy(alpha = gridColor.alpha * 0.7f),
            start = Offset(centerX - fDx, centerY - fDy),
            end = Offset(centerX + (dx - fDx), centerY + (dy - fDy)),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = gridColor.copy(alpha = gridColor.alpha * 0.7f),
            start = Offset(centerX + fDx, centerY - fDy),
            end = Offset(centerX - (dx - fDx), centerY + (dy - fDy)),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawIsometricServerUnit(
    centerX: Float,
    centerY: Float,
    width: Float,
    depth: Float,
    height: Float,
    layerIndex: Int,
    isRunning: Boolean,
    pulse: Float
) {
    val wHalf = width / 2f
    val dHalf = depth / 2f

    // Isometric top surface points
    val pTop = Offset(centerX, centerY - dHalf)
    val pRight = Offset(centerX + wHalf, centerY)
    val pBottom = Offset(centerX, centerY + dHalf)
    val pLeft = Offset(centerX - wHalf, centerY)

    // Extruded bottom surface points
    val pBottomRight = Offset(pRight.x, pRight.y + height)
    val pBottomBottom = Offset(pBottom.x, pBottom.y + height)
    val pBottomLeft = Offset(pLeft.x, pLeft.y + height)

    val activeGlow = if (isRunning) Cyan else Color(0xFF475569)
    val secondaryGlow = if (isRunning) Violet else Color(0xFF334155)

    // 1. Left Side Face
    val leftFace = Path().apply {
        moveTo(pLeft.x, pLeft.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pBottomBottom.x, pBottomBottom.y)
        lineTo(pBottomLeft.x, pBottomLeft.y)
        close()
    }
    drawPath(
        path = leftFace,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        style = Fill
    )
    drawPath(leftFace, color = activeGlow.copy(alpha = 0.4f), style = Stroke(width = 1.dp.toPx()))

    // 2. Right Side Face
    val rightFace = Path().apply {
        moveTo(pBottom.x, pBottom.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pBottomRight.x, pBottomRight.y)
        lineTo(pBottomBottom.x, pBottomBottom.y)
        close()
    }
    drawPath(
        path = rightFace,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF161F30), Color(0xFF0B1120))
        ),
        style = Fill
    )
    drawPath(rightFace, color = secondaryGlow.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))

    // 3. Top Face (Isometric plane)
    val topFace = Path().apply {
        moveTo(pTop.x, pTop.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pLeft.x, pLeft.y)
        close()
    }
    drawPath(
        path = topFace,
        brush = Brush.linearGradient(
            colors = listOf(
                if (isRunning) Color(0xFF1E293B) else Color(0xFF1B2333),
                if (isRunning) Color(0xFF0F172A) else Color(0xFF121826)
            ),
            start = pTop,
            end = pBottom
        ),
        style = Fill
    )
    drawPath(
        path = topFace,
        color = if (isRunning) Cyan.copy(alpha = 0.8f * pulse) else Color(0xFF475569),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 4. Server Bay LED Status Beacons on Front-Left Face
    val ledCount = 3
    for (led in 0 until ledCount) {
        val frac = (led + 1) / (ledCount + 1f)
        val ledX = pLeft.x + (pBottom.x - pLeft.x) * frac
        val ledY = pLeft.y + (pBottom.y - pLeft.y) * frac + height * 0.45f

        val ledColor = when {
            !isRunning -> Color(0xFF475569)
            led == 0 -> Cyan.copy(alpha = pulse)
            led == 1 -> Violet.copy(alpha = 1f - (pulse * 0.3f))
            else -> if (layerIndex == 0) Rose else Cyan
        }

        drawCircle(
            color = ledColor,
            radius = 2.5.dp.toPx(),
            center = Offset(ledX, ledY)
        )
    }
}

private fun DrawScope.drawDataPackets(
    topCenterX: Float,
    topCenterY: Float,
    progress: Float
) {
    // Upward radiating glowing cyan packets
    val beamCount = 3
    for (b in 0 until beamCount) {
        val beamOffsetProgress = (progress + (b / beamCount.toFloat())) % 1f
        val packetY = topCenterY - (beamOffsetProgress * 50.dp.toPx())
        val alpha = (1f - beamOffsetProgress).coerceIn(0f, 1f)

        drawCircle(
            color = Cyan.copy(alpha = alpha * 0.8f),
            radius = (3.dp.toPx() * (1f - beamOffsetProgress * 0.3f)),
            center = Offset(topCenterX, packetY)
        )
    }
}
