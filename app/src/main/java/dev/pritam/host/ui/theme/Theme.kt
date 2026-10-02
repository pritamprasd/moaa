package dev.pritam.host.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pritam.host.ftp.ui.components.LiquidGlassBackground
import dev.pritam.host.settings.AppSettingsManager

private val BaseDarkColorScheme = darkColorScheme(
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
    surface = GlassDialogSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    surfaceContainerLowest = Color.Transparent,
    surfaceContainerLow = GlassSurfaceDeep,
    surfaceContainer = GlassDialogSurface,
    surfaceContainerHigh = GlassDialogSurface,
    surfaceContainerHighest = GlassDialogSurface,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dialogOpacity by AppSettingsManager.dialogOpacityPercent.collectAsStateWithLifecycle()
    val dynamicDialogSurface = AppSettingsManager.getDialogSurfaceColor(dialogOpacity)

    val colorScheme = BaseDarkColorScheme.copy(
        surface = dynamicDialogSurface,
        surfaceContainer = dynamicDialogSurface,
        surfaceContainerHigh = dynamicDialogSurface,
        surfaceContainerHighest = dynamicDialogSurface,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
    ) {
        LiquidGlassBackground {
            content()
        }
    }
}