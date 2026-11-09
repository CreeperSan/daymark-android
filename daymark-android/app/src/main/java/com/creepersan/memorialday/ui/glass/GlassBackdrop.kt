package com.creepersan.memorialday.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

private val LightGlassBackdrop = listOf(Color(0xFFFFF2E3), Color(0xFFFFE2CE), Color(0xFFFFEEE3))
private val DarkGlassBackdrop = listOf(Color(0xFF201C2A), Color(0xFF322631), Color(0xFF211E2A))
private val LightHeaderShade = Color(0xFFFFE8D8)
private val DarkHeaderShade = Color(0xFF28212F)
private const val GLOW_WIDTH_RATIO = 0.68f
private const val GLOW_HEIGHT_RATIO = 0.32f
private const val HEADER_FADE_START = 0.75f
private const val HEADER_SHADE_OPACITY = 0.97f
private const val NAVIGATION_SHADE_OPACITY = 0.92f
private const val NAVIGATION_SHADE_MIDPOINT = 0.65f
private const val NAVIGATION_MIDPOINT_OPACITY = 0.82f
private const val SCRIM_FADE_DURATION_MS = 180

/** 背景光斑的中心按页面宽高比例定位，分别为明暗主题设置强度。 */
private class GlassGlow(val center: Offset, val light: Color, val dark: Color)

private val GlassGlows = listOf(
    GlassGlow(Offset(0.16f, 0.12f), Color(0x85FFA17D), Color(0x48FF9E73)),
    GlassGlow(Offset(0.88f, 0.37f), Color(0x48C9A6FF), Color(0x3BC9A6FF)),
    GlassGlow(Offset(0.82f, 0.69f), Color(0x64FFC46B), Color(0x36FFC46B))
)

/** 缓存暖色渐变和柔光。页面静止时没有持续的动画或逐帧绘制请求。 */
internal fun Modifier.glassBackdrop(dark: Boolean): Modifier = drawWithCache {
    // 窗口尺寸尚未确定时先跳过绘制，径向渐变需要正半径。
    if (size.isEmpty()) return@drawWithCache onDrawBehind { }
    val base = Brush.verticalGradient(if (dark) DarkGlassBackdrop else LightGlassBackdrop)
    val radius = maxOf(size.width * GLOW_WIDTH_RATIO, size.height * GLOW_HEIGHT_RATIO)
    val centers = GlassGlows.map { Offset(size.width * it.center.x, size.height * it.center.y) }
    val glows = GlassGlows.mapIndexed { index, glow ->
        val color = if (dark) glow.dark else glow.light
        Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), centers[index], radius)
    }
    onDrawBehind {
        drawRect(base)
        glows.forEachIndexed { index, brush -> drawCircle(brush, radius = radius, center = centers[index]) }
    }
}

/** 内容经过系统栏时淡入遮罩。动画每帧只更新绘制阶段的透明度。 */
@Composable
internal fun Modifier.glassScrollScrims(dark: Boolean, scrollState: ScrollableState, topHeight: Dp, bottomHeight: Dp): Modifier {
    val topOpacity = animateFloatAsState(if (scrollState.canScrollBackward) 1f else 0f, tween(SCRIM_FADE_DURATION_MS))
    val bottomOpacity = animateFloatAsState(if (scrollState.canScrollForward) 1f else 0f, tween(SCRIM_FADE_DURATION_MS))
    return drawWithCache {
        val top = topHeight.toPx().coerceAtMost(size.height)
        val bottom = bottomHeight.toPx().coerceAtMost(size.height)
        val bottomStart = size.height - bottom
        val headerColor = if (dark) DarkHeaderShade else LightHeaderShade
        val footerColor = if (dark) DarkGlassBackdrop.last() else LightGlassBackdrop.last()
        val headerShade = headerColor.copy(alpha = HEADER_SHADE_OPACITY)
        val header = Brush.verticalGradient(0f to headerShade, HEADER_FADE_START to headerShade, 1f to headerColor.copy(alpha = 0f), endY = top)
        val footer = Brush.verticalGradient(0f to footerColor.copy(alpha = 0f), NAVIGATION_SHADE_MIDPOINT to footerColor.copy(alpha = NAVIGATION_MIDPOINT_OPACITY), 1f to footerColor.copy(alpha = NAVIGATION_SHADE_OPACITY), startY = bottomStart, endY = size.height)
        val topSize = Size(size.width, top)
        val bottomOffset = Offset(0f, bottomStart)
        val bottomSize = Size(size.width, bottom)
        onDrawWithContent {
            drawContent()
            val topAlpha = topOpacity.value
            val bottomAlpha = bottomOpacity.value
            if (topAlpha > 0f) drawRect(header, size = topSize, alpha = topAlpha)
            if (bottomAlpha > 0f) drawRect(footer, topLeft = bottomOffset, size = bottomSize, alpha = bottomAlpha)
        }
    }
}
