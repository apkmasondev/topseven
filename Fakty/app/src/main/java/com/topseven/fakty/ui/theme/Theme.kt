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

@Composable
fun FaktyTheme(
    // We enforce DarkTheme for the premium WOW effect
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+, but we want our custom colors for WOW effect
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme



    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
