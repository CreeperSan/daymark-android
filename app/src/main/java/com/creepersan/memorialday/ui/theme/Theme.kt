package com.creepersan.memorialday.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun MemorialDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CSTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    ) {
        val view = LocalView.current
        val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        content()
    }
}