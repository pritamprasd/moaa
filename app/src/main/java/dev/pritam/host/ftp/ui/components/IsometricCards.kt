package dev.pritam.host.ftp.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassBorderHighlight
import dev.pritam.host.ui.theme.GlassSurface
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.GlassSurfaceElevated
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SpaceBackground
import dev.pritam.host.ui.theme.SpaceBackgroundEnd
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

/**
 * Clean Hairline Border Brushes for plain minimalist dark mode.
 * Subtle 1px neutral dividers without fluorescent rainbow glare.
 */
val RainbowBorderBrush = Brush.linearGradient(
    listOf(
        Color(0xFF222531),
        Color(0xFF2D3142),
        Color(0xFF222531)
    )
)

val RainbowGlassBorderBrush = Brush.linearGradient(
    listOf(
        Color(0xFF222531),
        Color(0xFF2D3142),
        Color(0xFF222531)
    )
)

val RainbowSoftGlowBrush = Brush.linearGradient(
    listOf(
        Color(0x10222531),
        Color(0x102D3142),
        Color(0x10222531)
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
                        Color(0xFF0B0C11),
                        SpaceBackgroundEnd
                    )
                )
            )
    ) {
        content()
    }
}

/**
 * Plain Minimalist Dark Card with clean 1px hairline border and matte elevation.
 */
@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Cyan,
    elevationDepth: Dp = 2.dp,
    useRainbowBorder: Boolean = false,
    content: @Composable () -> Unit,
) {
    val cornerRadius = 12.dp
    val shape = RoundedCornerShape(cornerRadius)

    val borderStroke = if (useRainbowBorder) {
        BorderStroke(1.dp, RainbowGlassBorderBrush)
    } else {
        BorderStroke(1.dp, GlassBorder)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(GlassSurface)
                .border(borderStroke, shape)
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

/**
 * Dedicated Minimalist Card with subtle 1px hairline border.
 */
@Composable
fun RainbowGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    elevationDepth: Dp = 2.dp,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(GlassSurface)
                .border(BorderStroke(1.dp, GlassBorder), shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = Cyan)
                        ) { onClick() }
                    } else Modifier
                )
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

/**
 * Plain Minimalist Dark Action Button:
 * Matte elevated surface with crisp 1px outline and high-contrast typography.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    glowColor: Color = Cyan,
    useRainbowBorder: Boolean = false,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: (@Composable () -> Unit)? = null,
) {
    val borderStroke = BorderStroke(
        1.dp,
        if (!enabled) Color(0xFF1C1E28) else Color(0xFF2E3345)
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() }
            .border(borderStroke, shape),
        color = if (enabled) GlassSurfaceElevated else Color(0xFF101117),
        shape = shape
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Minimalist Dark Metric Display Tile with clean typography and monospace values.
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
            .background(GlassSurface)
            .border(BorderStroke(1.dp, GlassBorder), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                fontSize = 9.5.sp
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
    val shape = RoundedCornerShape(6.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) activeColor else GlassBorder
                ),
                shape
            ),
        color = if (isSelected) Color(0xFF202330) else GlassSurface,
        shape = shape
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Plain minimalist dark back button with clean 1px hairline border.
 */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    contentDescription: String = "Back"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GlassSurfaceElevated)
                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

