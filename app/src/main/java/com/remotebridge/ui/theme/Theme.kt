package com.remotebridge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// لوحة ألوان مستوحاة من لون "التيل الأخضر المائل للأزرق" (Teal)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6E5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5F0E0),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4C6359),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8DA),
    onSecondaryContainer = Color(0xFF092018),
    tertiary = Color(0xFF426278),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF404944),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D4C4),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFA5F0E0),
    secondary = Color(0xFFB1CCBF),
    onSecondary = Color(0xFF1C352C),
    secondaryContainer = Color(0xFF344C42),
    onSecondaryContainer = Color(0xFFCDE8DA),
    tertiary = Color(0xFFA9CAF2),
    onTertiary = Color(0xFF0F3145),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E4E0),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE0E4E0),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun BridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
