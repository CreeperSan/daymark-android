package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

data class CSSelectionColors(
    val selected: Color = Color.Unspecified,
    val unselected: Color = Color.Unspecified,
    val checkmark: Color = Color.Unspecified
)

@Composable
fun CSCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    colors: CSSelectionColors = CSSelectionColors(),
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val active = enabled && onCheckedChange != null
    val selected = colors.selected.takeOrDefault(CSTheme.colors.primary)
    val unselected = colors.unselected.takeOrDefault(CSTheme.colors.border)
    val mark = colors.checkmark.takeOrDefault(Color.White)
    val selectionProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(180),
        label = "cs_checkbox_selection"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (checked) selected else unselected,
        animationSpec = tween(180),
        label = "cs_checkbox_color"
    )

    Row(
        modifier = modifier
            .csMinimumTouchTarget()
            .semantics {
                role = Role.Checkbox
                stateDescription = if (checked) "已选中" else "未选中"
            }
            .then(
                if (active) {
                    Modifier.csNoRippleClickable(role = Role.Checkbox) {
                        onCheckedChange?.invoke(!checked)
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(20.dp)) {
            drawRoundRect(
                color = animatedColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(CSShapes.checkbox.toPx()),
                style = if (selectionProgress > 0f) {
                    androidx.compose.ui.graphics.drawscope.Fill
                } else {
                    Stroke(1.5.dp.toPx())
                }
            )
            if (selectionProgress > 0f) {
                val path = Path().apply {
                    moveTo(size.width * 0.23f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.72f)
                    lineTo(size.width * 0.78f, size.height * 0.31f)
                }
                drawPath(
                    path = path,
                    color = mark.copy(alpha = selectionProgress),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        label?.let { CSTextBody(it) }
        content?.invoke(this)
    }
}

private fun Color.takeOrDefault(default: Color): Color = if (this == Color.Unspecified) default else this
