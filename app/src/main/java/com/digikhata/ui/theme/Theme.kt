package com.digikhata.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DigiColorScheme = lightColorScheme(
    primary = DigiRed,
    onPrimary = Color.White,
    secondary = DigiGreen,
    onSecondary = Color.White,
    error = DigiError,
    background = DigiBg,
    surface = DigiSurface,
    onSurface = DigiOnSurface,
    onSurfaceVariant = DigiOnSurfaceVariant,
    outline = DigiOutline
)

@Composable
fun DigiKhataTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DigiRedDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DigiColorScheme,
        typography = DigiTypography,
        content = content
    )
}
