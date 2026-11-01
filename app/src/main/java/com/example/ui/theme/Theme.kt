package com.example.nexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusDarkColorScheme = darkColorScheme(
    primary = NexusBlue,
    onPrimary = Color(0xFF041021),
    primaryContainer = NexusBlueDark,
    onPrimaryContainer = NexusText,
    secondary = NexusCyan,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF13383B),
    onSecondaryContainer = NexusCyan,
    tertiary = NexusAmber,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = NexusAmberDark,
    onTertiaryContainer = NexusAmber,
    background = NexusBg,
    onBackground = NexusText,
    surface = NexusPanel,
    onSurface = NexusText,
    surfaceVariant = NexusPanel2,
    onSurfaceVariant = NexusMuted,
    outline = NexusBorder,
    outlineVariant = NexusBorderFocus,
    error = NexusRed,
    onError = Color(0xFF380808),
    errorContainer = NexusRedDark,
    onErrorContainer = NexusRed
)

@Composable
fun NexusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        typography = Typography,
        content = content
    )
}
