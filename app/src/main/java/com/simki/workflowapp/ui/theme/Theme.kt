package com.simki.workflowapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    secondary = Coral60,
    onSecondary = Color.White,
    tertiary = Lemon60,
    onTertiary = Color.Black,
    background = Color(0xFF121212), // Dark background
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1A1E1E), // Darker surface
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = AccentPink,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Teal80,
    onPrimary = Color.Black,
    secondary = Coral80,
    onSecondary = Color.Black,
    tertiary = Lemon80,
    onTertiary = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF424242),
    error = AccentPink,
    onError = Color.White
)

@Composable
fun WorkflowAppTheme(
    dynamicColor: Boolean = false, // Disable dynamic color to enforce dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use dark theme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ContentStyle,
        content = content
    )
}
