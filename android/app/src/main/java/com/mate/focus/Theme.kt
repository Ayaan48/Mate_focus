package com.mate.focus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF12141A)
val Card = Color(0xFF1A1D26)
val Field = Color(0xFF232734)
val Chalk = Color(0xFFE8EAF0)
val Muted = Color(0xFF8B91A3)
val Accent = Color(0xFF5B8CFF)
val Good = Color(0xFF3ECF8E)
val Warn = Color(0xFFF5A623)
val Bad = Color(0xFFFF5D5D)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0A1020),
    secondary = Good,
    background = Ink,
    onBackground = Chalk,
    surface = Card,
    onSurface = Chalk,
    surfaceVariant = Field,
    onSurfaceVariant = Muted,
    error = Bad,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F5BD0),
    background = Color(0xFFF7F8FB),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun MateTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
