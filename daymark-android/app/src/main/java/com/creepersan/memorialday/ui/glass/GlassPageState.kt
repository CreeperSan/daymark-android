package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creepersan.memorialday.R

/** 首页等页面共用的空数据、断网和加载失败状态。 */
enum class GlassPageStateKind { Empty, Offline, Failed }

private val PageStateIconColor = Color(0xFFD4B6A1)
private val PageStateAccentColor = Color(0xFFE7A093)
private val PageStateIconSize = 48.dp
private val PageStateGap = 10.dp

/** 状态直接显示在页面背景上，宽度随容器变化，不包含卡片。 */
@Composable
fun GlassPageState(kind: GlassPageStateKind, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val title = stringResource(when (kind) { GlassPageStateKind.Empty -> R.string.page_state_empty_title; GlassPageStateKind.Offline -> R.string.page_state_offline_title; GlassPageStateKind.Failed -> R.string.page_state_failed_title })
    val description = stringResource(when (kind) { GlassPageStateKind.Empty -> R.string.page_state_empty_description; GlassPageStateKind.Offline -> R.string.page_state_offline_description; GlassPageStateKind.Failed -> R.string.page_state_failed_description })
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PageStateGap)) {
        PageStateIcon(kind)
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        Text(description, fontSize = 14.sp, lineHeight = 21.sp, color = glassSecondaryColor().copy(alpha = 0.9f), textAlign = TextAlign.Center)
        if (onRetry != null) Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 15.sp, modifier = Modifier.glassClickable(onClick = onRetry).padding(8.dp))
    }
}

/** 用矢量线条绘制设计稿图标，避免把页面宽度和图标资源绑定到设计稿尺寸。 */
@Composable
private fun PageStateIcon(kind: GlassPageStateKind) {
    Canvas(Modifier.size(PageStateIconSize)) {
        val unit = size.width / 32f
        val stroke = 2f * unit
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        when (kind) {
            GlassPageStateKind.Empty -> {
                drawRoundRect(PageStateIconColor, topLeft = point(6f, 11f), size = Size(20f * unit, 15f * unit), cornerRadius = CornerRadius(4f * unit), style = Stroke(stroke))
                drawLine(PageStateIconColor, point(6f, 16f), point(26f, 16f), stroke)
                drawArc(PageStateIconColor, 180f, 180f, false, topLeft = point(12f, 6f), size = Size(8f * unit, 10f * unit), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            GlassPageStateKind.Offline -> {
                drawArc(PageStateIconColor, 225f, 90f, false, topLeft = point(5f, 8f), size = Size(22f * unit, 17f * unit), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(PageStateIconColor, 225f, 90f, false, topLeft = point(10f, 14f), size = Size(12f * unit, 10f * unit), style = Stroke(stroke, cap = StrokeCap.Round))
                drawCircle(PageStateIconColor, radius = 1.8f * unit, center = point(16f, 22f))
                drawLine(PageStateAccentColor, point(7f, 6f), point(25f, 26f), stroke, cap = StrokeCap.Round)
            }
            GlassPageStateKind.Failed -> {
                drawCircle(PageStateIconColor, radius = 10.5f * unit, center = point(16f, 16f), style = Stroke(stroke))
                drawLine(PageStateAccentColor, point(16f, 10.6f), point(16f, 16.8f), stroke, cap = StrokeCap.Round)
                drawCircle(PageStateAccentColor, radius = 1.4f * unit, center = point(16f, 21.4f))
            }
        }
    }
}
