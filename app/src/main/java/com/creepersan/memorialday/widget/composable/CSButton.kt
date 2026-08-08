package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

private enum class CSButtonStyle {
    Primary,
    Secondary,
    Text
}

@Composable
fun CSButton(
    text: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    CSButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        small = small,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
        style = CSButtonStyle.Primary
    )
}

@Composable
fun CSButtonSecondary(
    text: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    CSButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        small = small,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
        style = CSButtonStyle.Secondary
    )
}

@Composable
fun CSButtonText(
    text: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    CSButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        small = small,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
        style = CSButtonStyle.Text
    )
}

@Composable
fun CSButtonLoading(
    text: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    CSButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = false,
        small = small,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
        style = CSButtonStyle.Primary,
        loading = enabled
    )
}

@Composable
private fun CSButtonSurface(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    small: Boolean,
    fullWidth: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    content: (@Composable RowScope.() -> Unit)?,
    style: CSButtonStyle,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.5f else 1f,
        animationSpec = tween(durationMillis = if (pressed && enabled) 50 else 200),
        label = "cs_button_press_alpha"
    )
    val shape = RoundedCornerShape(CSShapes.button)
    val height: Dp = if (small) 36.dp else 48.dp
    val baseColor = when (style) {
        CSButtonStyle.Primary -> CSTheme.colors.primary
        CSButtonStyle.Secondary -> CSTheme.colors.surface
        CSButtonStyle.Text -> Color.Transparent
    }
    val contentColor = when (style) {
        CSButtonStyle.Primary -> Color.White
        CSButtonStyle.Secondary,
        CSButtonStyle.Text -> CSTheme.colors.primary
    }
    val actualColor = if (enabled || loading) baseColor else CSTheme.colors.surfaceSubtle
    val actualContentColor = if (enabled || loading) contentColor else CSTheme.colors.textMuted
    val clickModifier = if (enabled) {
        Modifier.csNoRippleClickable(
            role = Role.Button,
            interactionSource = interactionSource,
            onClick = onClick
        )
    } else {
        Modifier
    }
    val widthModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier

    Box(
        modifier = modifier
            .graphicsLayer { alpha = pressAlpha }
            .then(widthModifier)
            .csMinimumTouchTarget()
            .defaultMinSize(minHeight = height)
            .background(actualColor, shape)
            .then(
                if (style == CSButtonStyle.Secondary) {
                    Modifier.border(1.dp, CSTheme.colors.border, shape)
                } else {
                    Modifier
                }
            )
            .then(clickModifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) {
                CSButtonSpinner(color = actualContentColor)
            }
            leadingIcon?.invoke()
            text?.let { Text(it, color = actualContentColor) }
            content?.invoke(this)
            trailingIcon?.invoke()
        }
    }
}

@Composable
private fun CSButtonSpinner(color: Color) {
    val transition = rememberInfiniteTransition(label = "cs_button_spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cs_button_spinner_rotation"
    )
    Canvas(Modifier.size(16.dp).graphicsLayer { rotationZ = rotation }) {
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
