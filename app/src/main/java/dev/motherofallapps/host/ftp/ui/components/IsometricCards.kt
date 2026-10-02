package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

/**
 * 3D Isometric Card with layered depth shadow, beveled surface, and glowing border.
 */
@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Cyan,
    elevationDepth: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val cornerRadius = 14.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Isometric 3D Base Drop Shadow / Depth Extrusion
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = elevationDepth, y = elevationDepth)
                .clip(shape)
                .background(Color(0xFF070A10))
                .border(
                    BorderStroke(1.dp, glowColor.copy(alpha = 0.25f)),
                    shape
                )
        )

        // 2. Main Top Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                glowColor.copy(alpha = 0.6f),
                                Violet.copy(alpha = 0.3f),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                    shape
                ),
            shape = shape,
            color = SurfaceElevated.copy(alpha = 0.95f),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Isometric Metric Display Tile for network speed, connected clients, or storage info.
 */
@Composable
fun IsometricStatTile(
    label: String,
    value: String,
    unit: String? = null,
    accentColor: Color = Cyan,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(SurfaceDeep.copy(alpha = 0.85f))
            .border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                )
                if (unit != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
