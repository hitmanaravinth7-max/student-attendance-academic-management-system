package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkBloodRed,
    onPrimary = Color.White,
    primaryContainer = DarkBloodContainer,
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = EmergencyOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF422000),
    onSecondaryContainer = Color(0xFFFFE0B2),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    outline = Color(0xFF44444E)
)

private val LightColorScheme = lightColorScheme(
    primary = BloodRedPrimary,
    onPrimary = Color.White,
    primaryContainer = BloodRedContainer,
    onPrimaryContainer = OnBloodRedContainer,
    secondary = EmergencyOrange,
    onSecondary = Color.White,
    secondaryContainer = EmergencyOrangeContainer,
    onSecondaryContainer = EmergencyOrange,
    background = MedicalBackground,
    surface = MedicalSurface,
    surfaceVariant = Color(0xFFF1F3F5),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = MedicalCardBorder
)

@Composable
fun LifeLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
