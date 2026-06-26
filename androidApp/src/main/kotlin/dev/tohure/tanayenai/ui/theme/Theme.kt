package dev.tohure.tanayenai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = PrimaryGreen,
        onPrimary = SurfaceColor,
        secondary = SecondaryMint,
        onSecondary = TextDark,
        tertiary = AccentTerra,
        background = BackgroundColor,
        onBackground = TextDark,
        surface = SurfaceColor,
        onSurface = TextDark,
        error = ErrorRed,
    )

@Composable
fun TanayenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = TanayenTypography,
        content = content,
    )
}
