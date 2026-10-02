package dev.pritam.host.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.pritam.host.ftp.ui.components.LiquidGlassBackground

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF041822),
    primaryContainer = CyanDim,
    onPrimaryContainer = Color(0xFFCFF3FF),
    secondary = Violet,
    onSecondary = Color(0xFF20163E),
    secondaryContainer = Color(0xFF2E2452),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Rose,
    onTertiary = Color(0xFF380820),
    background = Color.Transparent,
    onBackground = TextPrimary,
    surface = GlassSurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    surfaceContainerLowest = Color.Transparent,
    surfaceContainerLow = GlassSurfaceDeep,
    surfaceContainer = GlassSurface,
    surfaceContainerHigh = GlassSurfaceElevated,
    surfaceContainerHighest = Color(0x551E293B),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
    ) {
        LiquidGlassBackground {
            content()
        }
    }
}