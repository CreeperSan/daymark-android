package com.creepersan.memorialday.ui.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlassRimWidth = 1.dp
private const val GLASS_RIM_FADE_STOP = 0.58f
private val LightGlassTint = listOf(Color(0xC7FFFAF5), Color(0x58FFFAF7))
private val DarkGlassTint = listOf(Color(0x545F4D64), Color(0x40292236))
private val LightGlassRim = listOf(Color(0xFAFFFFFF), Color(0x8CFFFFFF), Color(0x55B77551))
private val DarkGlassRim = listOf(Color(0x7DFFE5D9), Color(0x33E0C3ED), Color(0x578C6A9C))
private val HighlightedGlassRim = listOf(Color(0xFFFFAE8F), Color(0xFFFF987B), Color(0xFFFF7E60))

/**
 * 用透色渐变和不均匀的边缘反光表现玻璃厚度。背景本身是柔光，无需为每块卡片采样或模糊。
 * 画刷和圆角随尺寸缓存，滚动时不重建，也不需要额外的离屏图层。
 */
internal fun Modifier.glassSurface(cornerRadius: Dp, dark: Boolean, highlighted: Boolean = false): Modifier = drawWithCache {
    val fill = Brush.verticalGradient(if (dark) DarkGlassTint else LightGlassTint)
    val rimColors = when {
        highlighted -> HighlightedGlassRim
        dark -> DarkGlassRim
        else -> LightGlassRim
    }
    val rim = Brush.linearGradient(0f to rimColors[0], GLASS_RIM_FADE_STOP to rimColors[1], 1f to rimColors[2])
    val radius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
    val strokeWidth = GlassRimWidth.toPx().coerceAtMost(size.minDimension / 2f)
    val stroke = Stroke(strokeWidth)
    val inset = strokeWidth / 2f
    // 描边沿内缘绘制，避免圆角外溢或被列表边界裁切。
    val rimOffset = Offset(inset, inset)
    val rimSize = Size(size.width - strokeWidth, size.height - strokeWidth)
    val rimRadius = CornerRadius((radius - inset).coerceAtLeast(0f))
    val fillRadius = CornerRadius(radius)
    onDrawBehind {
        drawRoundRect(fill, cornerRadius = fillRadius)
        drawRoundRect(rim, topLeft = rimOffset, size = rimSize, cornerRadius = rimRadius, style = stroke)
    }
}
