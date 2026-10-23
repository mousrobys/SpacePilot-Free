package com.spacepilot.free.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.spacepilot.free.data.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF002B2A),
    primaryContainer = Color(0xFF0D4F4D),
    onPrimaryContainer = Color(0xFFB9FFF8),
    secondary = Blue,
    onSecondary = Color(0xFF001E36),
    secondaryContainer = Color(0xFF123A5B),
    onSecondaryContainer = Color(0xFFD3E9FF),
    tertiary = Green,
    onTertiary = Color(0xFF003823),
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    outlineVariant = Color(0xFF1C3448),
    error = ErrorDark,
    onError = Color(0xFF3B0710),
    errorContainer = Color(0xFF5C1722),
    onErrorContainer = Color(0xFFFFDADD),
    inverseSurface = NightText,
    inverseOnSurface = Night,
    inversePrimary = LightTeal
)

private val LightColors = lightColorScheme(
    primary = LightTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F1EC),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = LightBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4FF),
    onSecondaryContainer = Color(0xFF001C35),
    tertiary = Color(0xFF006C4C),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightOutline,
    outlineVariant = Color(0xFFD3E3EC),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF18313F),
    inverseOnSurface = Color(0xFFECF5FA),
    inversePrimary = Teal
)

val SpacePilotShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
)

@Composable
fun SpacePilotTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SpacePilotTypography,
        shapes = SpacePilotShapes,
        content = content
    )
}
