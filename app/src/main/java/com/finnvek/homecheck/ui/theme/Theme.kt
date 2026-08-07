package com.finnvek.homecheck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.finnvek.homecheck.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF315A49),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E9DC),
    onPrimaryContainer = Color(0xFF17382B),
    secondary = Color(0xFF655F55),
    secondaryContainer = Color(0xFFEDE5D7),
    tertiary = Color(0xFF8A5A2B),
    tertiaryContainer = Color(0xFFFFDDBB),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFBF9F5),
    onBackground = Color(0xFF1C1C1A),
    surface = Color(0xFFFBF9F5),
    surfaceVariant = Color(0xFFE8E4DE),
    onSurfaceVariant = Color(0xFF494742),
    outline = Color(0xFF797872),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7D0B8),
    onPrimary = Color(0xFF123526),
    primaryContainer = Color(0xFF294C3C),
    onPrimaryContainer = Color(0xFFC3EBD3),
    secondary = Color(0xFFD2C6B5),
    secondaryContainer = Color(0xFF4C463D),
    tertiary = Color(0xFFF2BB7C),
    tertiaryContainer = Color(0xFF684018),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF111512),
    onBackground = Color(0xFFE3E4DF),
    surface = Color(0xFF111512),
    surfaceVariant = Color(0xFF424743),
    onSurfaceVariant = Color(0xFFC2C8C2),
    outline = Color(0xFF8C938D),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun HomeCheckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}

object HomeSpacing {
    val page = 20.dp
    val section = 24.dp
    val item = 12.dp
}
