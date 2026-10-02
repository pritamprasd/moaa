package dev.pritam.dynamictools.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SpaceBackground = Color(0xFF070B14)
val SurfaceDeep = Color(0xFF0E1626)
val SurfaceElevated = Color(0xFF162138)
val GlassBorder = Color(0x33475569)
val GlassSurfaceDeep = Color(0x280F172A)

val Cyan = Color(0xFF38BDF8)
val Rose = Color(0xFFF472B6)
val Violet = Color(0xFFA78BFA)
val Emerald = Color(0xFF34D399)
val Amber = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val RainbowGlassBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF38BDF8).copy(alpha = 0.6f),
        Color(0xFFA78BFA).copy(alpha = 0.6f),
        Color(0xFFF472B6).copy(alpha = 0.5f),
        Color(0xFF34D399).copy(alpha = 0.5f)
    )
)
