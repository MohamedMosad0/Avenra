package com.avenra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = WhiteColor,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = DarkNavy,
    background = BackgroundColor,
    onBackground = DarkNavy,
    surface = WhiteColor,
    onSurface = DarkNavy,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = ErrorColor,
    onError = WhiteColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = WhiteColor,
    background = DarkNavy,
    onBackground = WhiteColor,
    surface = DarkNavy,
    onSurface = WhiteColor,
    surfaceVariant = DarkNavy,
    onSurfaceVariant = WhiteColor,
    outline = Outline,
    error = ErrorColor,
    onError = WhiteColor,
)

@Composable
fun Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = DesignTypography,
        shapes = DesignShapes,
        content = content,
    )
}
