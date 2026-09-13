package dev.motherofallapps.host.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF062A36),
    primaryContainer = CyanDim,
    onPrimaryContainer = Color(0xFFCFF3FF),
    secondary = Violet,
    onSecondary = Color(0xFF2A2350),
    secondaryContainer = Color(0xFF3B3565),
    onSecondaryContainer = Color(0xFFE7E0FF),
    tertiary = Rose,
    onTertiary = Color(0xFF3B0F2E),
    background = SpaceBackground,
    onBackground = TextPrimary,
    surface = SurfaceDeep,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDeep,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDeep,
    surfaceContainerLowest = SpaceBackground,
    surfaceContainerLow = SurfaceDeep,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceVariantDeep,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}