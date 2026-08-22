package com.netquota.gateway.admin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val DeepInk = Color(0xFF10243E)
val SignalBlue = Color(0xFF176BFF)
val ElectricSky = Color(0xFF5CC8FF)
val Cloud = Color(0xFFF2F7FC)
val Paper = Color(0xFFFCFEFF)
val Amber = Color(0xFFF4A340)
val Danger = Color(0xFFE5484D)
val Muted = Color(0xFF65778C)
val Success = Color(0xFF1E9B72)

private val LightColors = lightColorScheme(
    primary = SignalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    onPrimaryContainer = DeepInk,
    secondary = ElectricSky,
    background = Cloud,
    onBackground = DeepInk,
    surface = Paper,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFE7EEF6),
    onSurfaceVariant = Muted,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = ElectricSky,
    onPrimary = DeepInk,
    secondary = SignalBlue,
    background = Color(0xFF081522),
    onBackground = Color(0xFFEAF4FF),
    surface = Color(0xFF10243E),
    onSurface = Color(0xFFEAF4FF),
    error = Color(0xFFFF6B70)
)

@Composable
fun NetQuotaGatewayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = DeepInk.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = NetQuotaTypography,
        content = content
    )
}
