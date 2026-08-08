package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSDividerStyle
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun CSDivider(
    modifier: Modifier = Modifier,
    style: CSDividerStyle = CSDividerStyle.Solid,
    label: String? = null,
    thickness: Dp = 1.dp,
    color: Color = CSTheme.colors.border
) {
    when (style) {
        CSDividerStyle.Solid -> CSDividerLine(modifier, thickness, color)
        CSDividerStyle.Dashed -> Canvas(modifier.fillMaxWidth().height(thickness)) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            )
        }
        CSDividerStyle.Label -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CSDividerLine(Modifier.fillMaxWidth(0.35f), thickness, color)
            CSTextHint(label.orEmpty())
            CSDividerLine(Modifier.fillMaxWidth(0.35f), thickness, color)
        }
    }
}

@Composable
private fun CSDividerLine(modifier: Modifier, thickness: Dp, color: Color) {
    Canvas(modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = thickness.toPx()
        )
    }
}
