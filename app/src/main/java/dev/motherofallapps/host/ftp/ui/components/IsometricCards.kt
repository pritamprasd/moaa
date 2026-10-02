package dev.motherofallapps.host.ftp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.text.input.VisualTransformation
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
 * Luminous Liquid Rainbow Gradient Brushes.
 * Flowing chromatic spectrum: Sky -> Indigo -> Violet -> Rose -> Amber -> Emerald -> Sky.
 */
val RainbowBorderBrush = Brush.linearGradient(
    listOf(
        Color(0xFF38BDF8), // Sky/Cyan
        Color(0xFF818CF8), // Indigo
        Color(0xFFA855F7), // Purple/Violet
        Color(0xFFEC4899), // Rose/Pink
        Color(0xFFF59E0B), // Amber/Gold
        Color(0xFF10B981), // Emerald/Green
        Color(0xFF38BDF8)  // Back to Sky/Cyan
    )
)

val RainbowGlassBorderBrush = Brush.linearGradient(
    listOf(
        Color(0xCC38BDF8), // Sky/Cyan @ 80%
        Color(0xCC818CF8), // Indigo @ 80%
        Color(0xCCA855F7), // Purple @ 80%
        Color(0xCCEC4899), // Rose @ 80%
        Color(0xCCF59E0B), // Amber @ 80%
        Color(0xCC10B981), // Emerald @ 80%
        Color(0xCC38BDF8)  // Back to Sky/Cyan
    )
)

val RainbowSoftGlowBrush = Brush.linearGradient(
    listOf(
        Color(0x5038BDF8),
        Color(0x50818CF8),
        Color(0x50A855F7),
        Color(0x50EC4899),
        Color(0x50F59E0B),
        Color(0x5010B981),
        Color(0x5038BDF8)
    )
)

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
 * Translucent Liquid Glass Card with clean shadow (no duplicate borders) and specular refraction border.
 */
@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Cyan,
    elevationDepth: Dp = 3.dp,
    useRainbowBorder: Boolean = false,
    content: @Composable () -> Unit,
) {
    val cornerRadius = 14.dp
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = if (useRainbowBorder) {
        RainbowGlassBorderBrush
    } else {
        Brush.linearGradient(
            listOf(
                GlassBorderHighlight,
                glowColor.copy(alpha = 0.65f),
                GlassBorder,
                glowColor.copy(alpha = 0.25f)
            )
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Clean Depth Shadow Backplate (WITHOUT duplicate border stroke)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = elevationDepth)
                .clip(shape)
                .background(Color(0x45030712))
        )

        // 2. Main Frosted Acrylic Glass Layer with Single Clean Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x3D1E293B), // Translucent frost top (~24%)
                            Color(0x1F0F172A), // Lighter translucent frost bottom (~12%)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, borderBrush),
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
                                if (useRainbowBorder) Color(0xCC38BDF8) else glowColor.copy(alpha = 0.5f),
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
 * Dedicated Rainbow Liquid Glass Card:
 * Clean, soft liquid shadow beneath + single radiant rainbow gradient border on frosted glass.
 */
@Composable
fun RainbowGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    elevationDepth: Dp = 3.dp,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // 1. Soft Liquid Glass Depth Shadow (Pure diffused dark glass, NO duplicate border!)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = elevationDepth)
                .clip(shape)
                .background(Color(0x55030712))
        )

        // 2. Frosted Acrylic Glass Card with Single Rainbow Gradient Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x401E293B), // Translucent frost (~25%)
                            Color(0x1C0F172A)  // Deep translucent frost (~11%)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, RainbowGlassBorderBrush),
                    shape
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = Cyan)
                        ) { onClick() }
                    } else Modifier
                )
        ) {
            // Top Specular Refraction Shine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.4f),
                                Color(0xFF38BDF8).copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner Content
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

/**
 * Liquid Glass Button:
 * Frosted translucent acrylic button with Rainbow or Accent Border and specular shine.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    glowColor: Color = Cyan,
    useRainbowBorder: Boolean = false,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: (@Composable () -> Unit)? = null,
) {
    val borderBrush = if (useRainbowBorder) {
        RainbowGlassBorderBrush
    } else {
        Brush.linearGradient(
            listOf(
                GlassBorderHighlight,
                if (enabled) glowColor else TextSecondary.copy(alpha = 0.4f),
                GlassBorder
            )
        )
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() }
            .border(BorderStroke(1.dp, borderBrush), shape),
        color = if (enabled) GlassSurfaceElevated else GlassSurfaceDeep,
        shape = shape
    ) {
        Box {
            // Specular Top Shine Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                if (content != null) {
                    content()
                } else if (text != null) {
                    Text(
                        text = text,
                        color = if (enabled) glowColor else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
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

/**
 * Standard Liquid Glass OutlinedTextField colors with translucent acrylic background and vibrant accent focus.
 */
@Composable
fun liquidGlassTextFieldColors(
    focusedBorderColor: Color = Cyan,
    unfocusedBorderColor: Color = GlassBorder,
    containerColor: Color = Color(0x350F172A)
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedPlaceholderColor = TextSecondary.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
        cursorColor = focusedBorderColor
    )
}

/**
 * Liquid Glass Filter / Tag Chip with glowing border when selected and translucent frosted fill.
 */
@Composable
fun LiquidGlassChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Cyan,
    useRainbowWhenSelected: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderBrush = when {
        isSelected && useRainbowWhenSelected -> RainbowGlassBorderBrush
        isSelected -> Brush.linearGradient(listOf(activeColor, activeColor.copy(alpha = 0.6f)))
        else -> Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(alpha = 0.4f)))
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
            .border(BorderStroke(1.dp, borderBrush), shape),
        color = if (isSelected) activeColor.copy(alpha = 0.16f) else GlassSurfaceDeep,
        shape = shape
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) activeColor else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }
}

