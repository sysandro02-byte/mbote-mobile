package com.loukatech.mbote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = PurpleDark,
    primaryContainer = PurpleMedium,
    onPrimaryContainer = PurpleSoft,
    secondary = TealAccent,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = Gray100,
    surface = DarkSurface,
    onSurface = Gray100,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Gray300,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PurpleSoft,
    onPrimaryContainer = PurpleDark,
    secondary = TealAccent,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = Gray50,
    onBackground = Gray900,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray200
)

@Composable
fun MBoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
