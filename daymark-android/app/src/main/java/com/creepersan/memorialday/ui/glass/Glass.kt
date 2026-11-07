package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.constance.AppTheme
import com.creepersan.memorialday.constance.KV_APPLICATION_THEME
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import androidx.compose.foundation.isSystemInDarkTheme

/** 新版页面统一的主题与渐变背景。 */
@Composable fun GlassPage(content: @Composable ColumnScope.() -> Unit) {
    val dark = when (KV.get(KV_APPLICATION_THEME)) { AppTheme.System -> isSystemInDarkTheme(); AppTheme.Dark -> true; AppTheme.Light -> false }
    MemorialDayTheme(darkTheme = dark, dynamicColor = false) {
        val colors = if (dark) listOf(Color(0xFF141D36), Color(0xFF30263C), Color(0xFF182B36)) else listOf(Color(0xFFE1EFF9), Color(0xFFF8E8E4), Color(0xFFE4F1E8))
        // 渐变背景并非 Material Surface，需要显式提供与主题匹配的前景色。
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxSize().background(Brush.linearGradient(colors)).statusBarsPadding().navigationBarsPadding().padding(24.dp), content = content)
        }
    }
}

/** 半透明卡片，以描边和柔和背景形成玻璃质感。 */
@Composable fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    // 卡片使用 surface 颜色，与页面背景分别提供对应的文字颜色。
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Color.White.copy(alpha = 0.45f), shape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.64f), shape).padding(20.dp), content = content)
    }
}

/** 新版页面通用操作按钮。 */
@Composable fun GlassButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(18.dp)) { Text(text, modifier = Modifier.padding(6.dp)) }
}
