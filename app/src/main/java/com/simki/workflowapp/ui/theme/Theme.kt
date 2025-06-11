package com.simki.workflowapp.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    secondary = Coral60,
    onSecondary = Color.White,
    tertiary = Lemon60,
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1A1E1E),
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

val LocalThemeState = compositionLocalOf<ThemeState> {
    error("No ThemeState provided")
}

data class ThemeState(val isDarkTheme: Boolean, val toggleTheme: () -> Unit)

@Composable
fun ProvideThemeState(
    isDarkTheme: Boolean,
    toggleTheme: () -> Unit,
    content: @Composable () -> Unit
) {
    val themeState = ThemeState(isDarkTheme, toggleTheme)
    androidx.compose.runtime.CompositionLocalProvider(LocalThemeState provides themeState) {
        content()
    }
}

@Composable
fun WorkflowAppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("WorkflowPrefs", Context.MODE_PRIVATE)
    val isDarkTheme = remember {
        mutableStateOf(sharedPreferences.getBoolean("isDarkTheme", true))
    }

    SideEffect {
        sharedPreferences.edit {
            putBoolean("isDarkTheme", isDarkTheme.value)
        }
    }

    val colorScheme = if (isDarkTheme.value) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme.value
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme.value
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ContentStyle,
        content = {
            ProvideThemeState(isDarkTheme = isDarkTheme.value, toggleTheme = { isDarkTheme.value = !isDarkTheme.value }) {
                content()
            }
        }
    )
}