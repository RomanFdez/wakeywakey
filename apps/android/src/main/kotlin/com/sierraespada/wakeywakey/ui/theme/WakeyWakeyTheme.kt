package com.sierraespada.wakeywakey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val Coral  = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary          = Yellow,
    onPrimary        = Navy,
    secondary        = Navy,
    onSecondary      = Color.White,
    tertiary         = Coral,
    background       = Color(0xFFF5F5F0),
    surface          = Color.White,
)

private val DarkColors = darkColorScheme(
    primary          = Yellow,
    onPrimary        = Navy,
    secondary        = Color.White,
    onSecondary      = Navy,
    tertiary         = Coral,
    background       = Navy,
    surface          = Color(0xFF16213E),
)

@Composable
fun WakeyWakeyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content,
    )
}
