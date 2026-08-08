package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSProgressStyle
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csClampedProgress

data class CSProgressColors(
    val track: Color = Color.Unspecified,
    val progress: Color = Color.Unspecified,
    val secondary: Color = Color.Unspecified
)

@Composable
fun CSProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    style: CSProgressStyle = CSProgressStyle.Linear,
    secondaryProgress: Float? = null,
    showPercent: Boolean = false,
    colors: CSProgressColors = CSProgressColors(),
    contentDescription: String? = null,
    size: Dp = 64.dp,
    completedOverlay: (@Composable () -> Unit)? = null
) {
    val clamped = progress.csClampedProgress()
    val secondary = secondaryProgress?.csClampedProgress()
    val animatedProgress = animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (CSTheme.motion.enabled) tween(durationMillis = 300) else snap(),
        label = "cs_progress_primary"
    ).value
    val animatedSecondary = if (secondary == null) {
        null
    } else {
        animateFloatAsState(
            targetValue = secondary,
            animationSpec = if (CSTheme.motion.enabled) tween(durationMillis = 300) else snap(),
            label = "cs_progress_secondary"
        ).value
    }
    val track = colors.track.takeOrDefault(CSTheme.colors.primarySoft)
    val primary = colors.progress.takeOrDefault(CSTheme.colors.primary)
    val secondaryColor = colors.secondary.takeOrDefault(CSTheme.colors.secondary)
    val description = contentDescription ?: "${(clamped * 100).toInt()}%"
    val semanticsModifier = modifier.semantics {
        this.contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
    }

    when (style) {
        CSProgressStyle.Linear -> Column(modifier = semanticsModifier) {
            Box(Modifier.fillMaxWidth().height(8.dp)) {
                Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                    val radius = this.size.height / 2f
                    drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
                    animatedSecondary?.let {
                        drawRoundRect(
                            secondaryColor,
                            size = androidx.compose.ui.geometry.Size(this.size.width * it, this.size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                        )
                    }
                    drawRoundRect(
                        primary,
                        size = androidx.compose.ui.geometry.Size(this.size.width * animatedProgress, this.size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                    )
                }
                if (animatedProgress >= 1f) {
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        completedOverlay?.invoke()
                    }
                }
            }
            if (showPercent) {
                CSTextHint("${(animatedProgress * 100).toInt()}%", color = CSTheme.colors.textSecondary)
            }
        }
        CSProgressStyle.Circular -> Box(
            modifier = semanticsModifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 6.dp.toPx()
                drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                animatedSecondary?.let {
                    drawArc(secondaryColor, -90f, 360f * it, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                drawArc(primary, -90f, 360f * animatedProgress, false, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (animatedProgress >= 1f && completedOverlay != null) {
                completedOverlay()
            } else if (showPercent) {
                CSTextHint("${(animatedProgress * 100).toInt()}%", color = CSTheme.colors.textPrimary)
            }
        }
    }
}

private fun Color.takeOrDefault(default: Color): Color = if (this == Color.Unspecified) default else this
