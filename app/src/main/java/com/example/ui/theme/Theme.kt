package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ChessAmberLight,
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = ChessWoodSecondary,
    onSecondary = Color.White,
    background = ChessDarkBackground,
    surface = ChessDarkSurface,
    surfaceVariant = ChessDarkSurfaceVariant,
    onBackground = ChessDarkOnSurface,
    onSurface = ChessDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = ChessAmberPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = ChessWoodSecondary,
    onSecondary = Color.White,
    background = ChessLightBackground,
    surface = ChessLightSurface,
    surfaceVariant = ChessLightSurfaceVariant,
    onBackground = ChessLightOnSurface,
    onSurface = ChessLightOnSurface
)

@Composable
fun ChessForTwoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
