package com.smartglasses.demo.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF00B4D8),
    onPrimary        = Color(0xFF001F28),
    primaryContainer = Color(0xFF003547),
    secondary        = Color(0xFF90CAF9),
    background       = Color(0xFF060D1B),
    surface          = Color(0xFF0E1F38),
    onBackground     = Color(0xFFF0F6FF),
    onSurface        = Color(0xFFCCE5FF),
    error            = Color(0xFFEF476F),
    onError          = Color.White
)

@Composable
fun SmartGlassesTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
