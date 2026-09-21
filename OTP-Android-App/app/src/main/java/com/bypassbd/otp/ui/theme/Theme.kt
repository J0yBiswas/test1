package com.bypassbd.otp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber      = Color(0xFFF5A524)
val AmberDark  = Color(0xFFB97C10)
val BgDark     = Color(0xFF0B0F16)
val PanelDark  = Color(0xFF111827)
val Panel2Dark = Color(0xFF161F2E)
val LineDark   = Color(0xFF2B3A52)
val TxtDark    = Color(0xFFE6EDF6)
val DimDark    = Color(0xFF8496AE)
val Ok         = Color(0xFF2ECC71)
val Err        = Color(0xFFFF5F56)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF12181F),
    secondary = AmberDark,
    background = BgDark,
    onBackground = TxtDark,
    surface = PanelDark,
    onSurface = TxtDark,
    surfaceVariant = Panel2Dark,
    onSurfaceVariant = DimDark,
    outline = LineDark,
    error = Err
)

private val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    secondary = Amber,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1A2130),
    surface = Color.White,
    onSurface = Color(0xFF1A2130),
    surfaceVariant = Color(0xFFEDF0F5),
    onSurfaceVariant = Color(0xFF5B6B82),
    outline = Color(0xFFCBD3DF),
    error = Err
)

@Composable
fun IvacOtpTheme(useDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
