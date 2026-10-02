package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.GlassBorder
import dev.motherofallapps.host.ui.theme.GlassBorderHighlight
import dev.motherofallapps.host.ui.theme.GlassSurface
import dev.motherofallapps.host.ui.theme.GlassSurfaceDeep
import dev.motherofallapps.host.ui.theme.GlassSurfaceElevated
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SpaceBackgroundEnd
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

/**
 * Global Liquid Glass Chromatic Ambient Mesh Background.
 * Renders glowing translucent fluid light orbs underneath frosted glass surfaces.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpaceBackground,
                        Color(0xFF090E1A),
                        SpaceBackgroundEnd
                    )
                )
            )
    ) {
        // Cyan / Sky Liquid Light Orb (Top-Left)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .clip(RoundedCornerShape(160.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            Cyan.copy(alpha = 0.18f),
                            Cyan.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Violet Neon Ambient Orb (Center-Right)
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp, y = 80.dp)
                .clip(RoundedCornerShape(180.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            Violet.copy(alpha = 0.22f),
                            Violet.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Rose / Emerald Chromatic Orb (Bottom-Left)
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 80.dp)
                .clip(RoundedCornerShape(170.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            Rose.copy(alpha = 0.14f),
                            Color(0xFF34D399).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main App Content rendered over liquid glass background
        content()
    }
}

/**
 * Translucent Liquid Glass Card with specular refraction borders, ambient glow, and depth extrusion.
 */
@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Cyan,
    elevationDepth: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val cornerRadius = 16.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Liquid Glass Refractive Backplate Depth Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = elevationDepth * 0.75f, y = elevationDepth)
                .clip(shape)
                .background(Color(0x55030712))
                .border(
                    BorderStroke(1.dp, glowColor.copy(alpha = 0.2f)),
                    shape
                )
        )

        // 2. Main Frosted Acrylic Glass Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x451E293B), // Translucent frost top
                            Color(0x280F172A), // Lighter translucent frost bottom
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                GlassBorderHighlight,
                                glowColor.copy(alpha = 0.65f),
                                GlassBorder,
                                glowColor.copy(alpha = 0.25f)
                            )
                        )
                    ),
                    shape
                )
        ) {
            // Specular Top Shine Glint Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                glowColor.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content Container with Padding
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Frosted Liquid Glass Metric Display Tile with luminous values and acrylic transparency.
 */
@Composable
fun IsometricStatTile(
    label: String,
    value: String,
    unit: String? = null,
    accentColor: Color = Cyan,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x351E293B),
                        Color(0x180F172A)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.3f),
                            accentColor.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                ),
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
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
