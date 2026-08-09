package com.topseven.fakty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    background = DarkBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkElevated,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

/**
 * Aplikacja celowo używa wyłącznie ciemnej palety - efekt "szkła" (Glassmorphism) opiera się
 * na jasnych tekstach i półprzezroczystych warstwach na ciemnym tle. Parametry `darkTheme`
 * i `dynamicColor` zostały usunięte, bo nigdy nie były odczytywane i sugerowały wsparcie,
 * którego nie ma.
 */
@Composable
fun FaktyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
