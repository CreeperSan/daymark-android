package com.creepersan.memorialday.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.runtime.CompositionLocalProvider
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

/** 保留点击语义，仅绘制内容，不绘制任何水波或高亮。 */
private object NoIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance = object : IndicationInstance {
        override fun ContentDrawScope.drawIndication() { drawContent() }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFA486),
    secondary = androidx.compose.ui.graphics.Color(0xFFE2B7AD),
    tertiary = androidx.compose.ui.graphics.Color(0xFFE7C29C),
    background = androidx.compose.ui.graphics.Color(0xFF201C2A),
    surface = androidx.compose.ui.graphics.Color(0xFF393037),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8EBE7),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8EBE7)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF6A45),
    secondary = androidx.compose.ui.graphics.Color(0xFFAA6557),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB5764B),
    background = androidx.compose.ui.graphics.Color(0xFFFFF2E3),
    surface = androidx.compose.ui.graphics.Color(0xFFFFF8F3),
    onBackground = androidx.compose.ui.graphics.Color(0xFF4A2511),
    onSurface = androidx.compose.ui.graphics.Color(0xFF4A2511)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MemorialDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        // 禁止所有 Compose 页面默认的点击水波反馈。
        CompositionLocalProvider(LocalIndication provides NoIndication, content = content)
    }
}