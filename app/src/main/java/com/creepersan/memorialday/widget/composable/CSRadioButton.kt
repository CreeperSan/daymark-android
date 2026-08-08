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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    colors: CSSelectionColors = CSSelectionColors(),
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val active = enabled && onClick != null
    val selectedColor = colors.selected.takeOrDefault(CSTheme.colors.primary)
    val unselectedColor = colors.unselected.takeOrDefault(CSTheme.colors.border)
    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "cs_radio_selection"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(180),
        label = "cs_radio_color"
    )

    Row(
        modifier = modifier
            .csMinimumTouchTarget()
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "已选中" else "未选中"
            }
            .then(
                if (active) Modifier.csNoRippleClickable(role = Role.RadioButton, onClick = onClick)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(20.dp)) {
            drawCircle(color = animatedColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            if (selectionProgress > 0f) {
                drawCircle(
                    color = selectedColor,
                    radius = size.minDimension * 0.27f * selectionProgress
                )
            }
        }
        label?.let { CSTextBody(it) }
        content?.invoke(this)
    }
}

private fun Color.takeOrDefault(default: Color): Color = if (this == Color.Unspecified) default else this
