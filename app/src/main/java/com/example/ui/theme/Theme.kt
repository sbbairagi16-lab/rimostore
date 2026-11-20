package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PulseEmerald,
    onPrimary = Color.White,
    primaryContainer = PulseMintContainer,
    onPrimaryContainer = PulseMintOnContainer,
    secondary = PulseCobalt,
    onSecondary = Color.White,
    secondaryContainer = PulseCobaltContainer,
    onSecondaryContainer = PulseCobaltOnContainer,
    tertiary = PulseCoral,
    onTertiary = Color.White,
    tertiaryContainer = PulseCoralContainer,
    onTertiaryContainer = PulseCoralOnContainer,
    background = AlabasterBg,
    onBackground = MidnightInk,
    surface = PureSurface,
    onSurface = MidnightInk,
    surfaceVariant = SoftSlateSurface,
    onSurfaceVariant = MutedSlate,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = PulseEmeraldBright,
    onPrimary = Color(0xFF002114),
    primaryContainer = DarkMintContainer,
    onPrimaryContainer = DarkMintOnContainer,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0B1F4B),
    secondaryContainer = DarkCobaltContainer,
    onSecondaryContainer = DarkCobaltOnContainer,
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = DarkCoralContainer,
    onTertiaryContainer = DarkCoralOnContainer,
    background = ObsidianBg,
    onBackground = IceInk,
    surface = ObsidianSurface,
    onSurface = IceInk,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SoftSteel,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulseStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content
    )
}
