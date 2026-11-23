package com.ncs.nextbus.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo600,
    secondary = Emerald500,
    tertiary = Amber500,
    surface = Charcoal900,
    surfaceVariant = Charcoal800,
    background = Charcoal900,
    onPrimary = Surface0,
    onSecondary = Surface0,
    onTertiary = Surface0,
    onSurface = Surface0,
    onBackground = Surface0,
    onSurfaceVariant = Surface0.copy(alpha = 0.7f),
    outline = Charcoal700.copy(alpha = 0.3f),
    inverseSurface = Surface50,
    inverseOnSurface = Charcoal900
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    secondary = Emerald500,
    tertiary = Amber500,
    surface = Surface100,
    surfaceVariant = Surface50,
    background = Surface0,
    onPrimary = Surface0,
    onSecondary = Surface0,
    onTertiary = Surface0,
    onSurface = Charcoal900,
    onBackground = Charcoal900,
    onSurfaceVariant = Charcoal800,
    outline = Charcoal700.copy(alpha = 0.2f),
    inverseSurface = Charcoal900,
    inverseOnSurface = Surface0
)

@Composable
fun NextBusTheme(
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

object Shapes {
    val Small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val Medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val Large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val Full = androidx.compose.foundation.shape.CircleShape
    val Pill = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
}