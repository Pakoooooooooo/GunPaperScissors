package com.example.shifumiplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

private val DarkColorScheme = darkColorScheme(
    primary = DarkGrey,
    secondary = Blue,
    tertiary = LiteGrey,
    background = White,
)

private val LightColorScheme = lightColorScheme(
    primary = White,
    secondary = Blue,
    tertiary = LiteGrey,
    background = White,
    surface = White,
    onBackground = DarkGrey,
    onSurface = DarkGrey
)

@Composable
fun ShiFuMiPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Disabled by default so app colorScheme is consistent across devices
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        // Force default text/content color to secondary from the color scheme
        CompositionLocalProvider(LocalContentColor provides colorScheme.secondary) {
            Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                content()
            }
        }
    }
}