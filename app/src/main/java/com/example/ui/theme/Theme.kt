package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = Color.Black,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = GoldContainer,
    secondary = EmeraldContainer,
    onSecondary = OnEmeraldContainer,
    background = DarkPaperBackground,
    onBackground = TextUthmaniDark,
    surface = DarkPaperSurface,
    onSurface = TextUthmaniDark,
    surfaceVariant = DarkPaperBorder,
    outline = GoldDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldPrimary,
    onSecondary = Color.Black,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    background = ParchmentLight,
    onBackground = TextUthmani,
    surface = ParchmentSurface,
    onSurface = TextUthmani,
    surfaceVariant = ParchmentBorder,
    outline = GoldDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
