package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSLoadingVariant
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun CSLoading(
    modifier: Modifier = Modifier,
    variant: CSLoadingVariant = CSLoadingVariant.Spinner,
    size: Dp = 24.dp,
    label: String? = null,
    color: Color = CSTheme.colors.primary,
    reducedMotion: Boolean = !CSTheme.motion.enabled
) {
    Row(
        modifier = modifier.semantics { label?.let { contentDescription = it } },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (variant) {
            CSLoadingVariant.Spinner -> {
                if (reducedMotion) CSStaticSpinner(size, color) else CSAnimatedSpinner(size, color)
            }
            CSLoadingVariant.Dots -> {
                if (reducedMotion) CSStaticDots(size, color) else CSAnimatedDots(size, color)
            }
            CSLoadingVariant.Text -> {
                if (reducedMotion) CSStaticSpinner(size, color) else CSAnimatedSpinner(size, color)
                label?.let { CSTextBodySmall(it, color = CSTheme.colors.textSecondary) }
            }
        }
    }
}

@Composable
private fun CSAnimatedSpinner(indicatorSize: Dp, color: Color) {
    val transition = rememberInfiniteTransition(label = "cs_loading_spinner")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Restart),
        label = "cs_loading_rotation"
    ).value
    CSSpinnerCanvas(indicatorSize, color, rotation)
}

@Composable
private fun CSStaticSpinner(indicatorSize: Dp, color: Color) {
    CSSpinnerCanvas(indicatorSize, color, 0f)
}

@Composable
private fun CSSpinnerCanvas(indicatorSize: Dp, color: Color, rotation: Float) {
    Canvas(Modifier.size(indicatorSize)) {
        drawArc(
            color = color,
            startAngle = rotation - 50f,
            sweepAngle = 275f,
            useCenter = false,
            style = Stroke(width = indicatorSize.toPx() / 7f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CSAnimatedDots(indicatorSize: Dp, color: Color) {
    val transition = rememberInfiniteTransition(label = "cs_loading_dots")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "cs_dots_phase"
    ).value
    CSDotsCanvas(indicatorSize, color, phase)
}

@Composable
private fun CSStaticDots(indicatorSize: Dp, color: Color) {
    CSDotsCanvas(indicatorSize, color, 0f)
}

@Composable
private fun CSDotsCanvas(indicatorSize: Dp, color: Color, phase: Float) {
    Canvas(Modifier.size(width = indicatorSize * 2, height = indicatorSize)) {
        val radius = this.size.minDimension / 7f
        repeat(3) { index ->
            val distance = kotlin.math.abs((phase * 3f - index + 3f) % 3f - 1f)
            val alpha = (1f - distance * 0.55f).coerceIn(0.35f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(
                    x = this.size.width * (0.2f + index * 0.3f),
                    y = this.size.height / 2f
                )
            )
        }
    }
}
