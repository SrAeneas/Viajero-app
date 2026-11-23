package com.viajero.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF0E7C86)
val TealDark = Color(0xFF0A5A63)
val Coral = Color(0xFFFF6B6B)
val Sun = Color(0xFFFFC857)
val Bg = Color(0xFFF5F9FA)
val Ink = Color(0xFF12343B)
val Muted = Color(0xFF6B8087)

private val gradients = listOf(
    listOf(Color(0xFF0E7C86), Color(0xFF3FC1C9)),
    listOf(Color(0xFFFF6B6B), Color(0xFFFFA36B)),
    listOf(Color(0xFF7C5CFC), Color(0xFFB69CFF)),
    listOf(Color(0xFF2E9E6B), Color(0xFF8FD694)),
    listOf(Color(0xFF3A7BD5), Color(0xFF00D2FF)),
)

fun gradientFor(id: String): List<Color> = gradients[(id.hashCode() and 0x7fffffff) % gradients.size]

@Composable
fun ViajeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Coral,
            tertiary = Sun,
            background = Bg,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink,
        ),
        content = content,
    )
}
