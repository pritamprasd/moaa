package dev.pritam.host.tool.nfc.ui.components

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
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.Violet
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Isometric NFC Antenna Canvas with animated electromagnetic resonance rings.
 */
@Composable
fun IsometricNfcAntenna(
    isScanningOrWriting: Boolean,
    isWriteMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_anim")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waves"
    )

    Box(
        modifier = modifier.height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 10.dp.toPx()

            val primaryColor = if (isWriteMode) Rose else Cyan
            val secondaryColor = if (isWriteMode) Violet else Cyan

            // 1. Draw 3 concentric isometric antenna copper coil loops
            drawIsometricCoils(cx, cy, primaryColor, pulse)

            // 2. Draw Center Silicon IC Chip Die in 3D Isometric
            drawIsometricChipDie(cx, cy, isWriteMode, pulse)

            // 3. Draw Radiating Magnetic Field Waves
            if (isScanningOrWriting) {
                drawRadiatingFluxWaves(cx, cy, waveProgress, primaryColor)
            }
        }
    }
}

private fun DrawScope.drawIsometricCoils(
    centerX: Float,
    centerY: Float,
    color: Color,
    pulse: Float,
) {
    val angle = 30f * (Math.PI.toFloat() / 180f)
    val cosA = cos(angle)
    val sinA = sin(angle)

    val baseRadius = 75.dp.toPx()

    for (loop in 0..2) {
        val r = baseRadius - (loop * 14.dp.toPx())
        val dx = cosA * r
        val dy = sinA * r

        val coilPath = Path().apply {
            moveTo(centerX, centerY - dy)
            lineTo(centerX + dx, centerY)
            lineTo(centerX, centerY + dy)
            lineTo(centerX - dx, centerY)
            close()
        }

        val alpha = if (loop == 0) 0.8f * pulse else 0.4f * (1f - (loop * 0.15f))
        drawPath(
            path = coilPath,
            color = color.copy(alpha = alpha),
            style = Stroke(width = (2.5f - loop * 0.5f).dp.toPx())
        )
    }
}

private fun DrawScope.drawIsometricChipDie(
    centerX: Float,
    centerY: Float,
    isWriteMode: Boolean,
    pulse: Float,
) {
    val chipWidth = 28.dp.toPx()
    val chipDepth = 18.dp.toPx()
    val chipHeight = 8.dp.toPx()

    val wHalf = chipWidth / 2f
    val dHalf = chipDepth / 2f

    val pTop = Offset(centerX, centerY - dHalf)
    val pRight = Offset(centerX + wHalf, centerY)
    val pBottom = Offset(centerX, centerY + dHalf)
    val pLeft = Offset(centerX - wHalf, centerY)

    val pBottomRight = Offset(pRight.x, pRight.y + chipHeight)
    val pBottomBottom = Offset(pBottom.x, pBottom.y + chipHeight)
    val pBottomLeft = Offset(pLeft.x, pLeft.y + chipHeight)

    val activeColor = if (isWriteMode) Rose else Cyan

    // Left face
    val leftFace = Path().apply {
        moveTo(pLeft.x, pLeft.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pBottomBottom.x, pBottomBottom.y)
        lineTo(pBottomLeft.x, pBottomLeft.y)
        close()
    }
    drawPath(leftFace, brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), style = Fill)
    drawPath(leftFace, color = activeColor.copy(alpha = 0.4f), style = Stroke(1.dp.toPx()))

    // Right face
    val rightFace = Path().apply {
        moveTo(pBottom.x, pBottom.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pBottomRight.x, pBottomRight.y)
        lineTo(pBottomBottom.x, pBottomBottom.y)
        close()
    }
    drawPath(rightFace, brush = Brush.verticalGradient(listOf(Color(0xFF161F30), Color(0xFF0B1120))), style = Fill)
    drawPath(rightFace, color = activeColor.copy(alpha = 0.3f), style = Stroke(1.dp.toPx()))

    // Top face
    val topFace = Path().apply {
        moveTo(pTop.x, pTop.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pLeft.x, pLeft.y)
        close()
    }
    drawPath(topFace, color = Color(0xFF0F172A), style = Fill)
    drawPath(topFace, color = activeColor.copy(alpha = 0.9f * pulse), style = Stroke(1.5.dp.toPx()))

    // Silicon Center Core Glow Dot
    drawCircle(
        color = activeColor.copy(alpha = pulse),
        radius = 3.dp.toPx(),
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawRadiatingFluxWaves(
    centerX: Float,
    centerY: Float,
    progress: Float,
    color: Color,
) {
    val angle = 30f * (Math.PI.toFloat() / 180f)
    val cosA = cos(angle)
    val sinA = sin(angle)

    val maxR = 95.dp.toPx()

    for (w in 0..1) {
        val p = (progress + (w * 0.5f)) % 1f
        val r = p * maxR
        val dx = cosA * r
        val dy = sinA * r
        val alpha = (1f - p).coerceIn(0f, 1f) * 0.7f

        val wavePath = Path().apply {
            moveTo(centerX, centerY - dy)
            lineTo(centerX + dx, centerY)
            lineTo(centerX, centerY + dy)
            lineTo(centerX - dx, centerY)
            close()
        }

        drawPath(wavePath, color = color.copy(alpha = alpha), style = Stroke(1.5.dp.toPx()))
    }
}
