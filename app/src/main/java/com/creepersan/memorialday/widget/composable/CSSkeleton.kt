package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun CSSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CSShapes.input),
    color: Color = CSTheme.colors.surfaceSubtle,
    highlightColor: Color = CSTheme.colors.surface,
    animated: Boolean = CSTheme.motion.enabled
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
    ) {
        if (animated) {
            CSShimmerOverlay(highlightColor)
        }
    }
}

@Composable
fun CSSkeletonCard(
    modifier: Modifier = Modifier,
    animated: Boolean = CSTheme.motion.enabled,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(CSShapes.card)
    val shimmerOffset = if (animated) {
        val transition = rememberInfiniteTransition(label = "cs_skeleton_card_shimmer")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
            label = "cs_skeleton_card_offset"
        ).value
    } else {
        0f
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CSTheme.colors.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (animated) {
                        Modifier
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .csSkeletonShimmerMask(
                                offset = shimmerOffset,
                                highlightColor = CSTheme.colors.surface.copy(alpha = 0.68f)
                            )
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CSSkeleton(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    animated = false
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CSSkeleton(Modifier.fillMaxWidth(0.72f).height(16.dp), animated = false)
                    CSSkeleton(Modifier.fillMaxWidth(0.46f).height(12.dp), animated = false)
                }
                content?.invoke(this)
            }
        }
    }
}

@Composable
private fun CSShimmerOverlay(highlightColor: Color) {
    val transition = rememberInfiniteTransition(label = "cs_skeleton_shimmer")
    val offset = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
        label = "cs_skeleton_offset"
    ).value
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(width * (offset - 1f), 0f),
                end = androidx.compose.ui.geometry.Offset(width * offset, size.height)
            )
        )
    }
}

private fun Modifier.csSkeletonShimmerMask(offset: Float, highlightColor: Color): Modifier =
    drawWithCache {
        val width = size.width
        val brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset(width * (offset - 1f), 0f),
            end = androidx.compose.ui.geometry.Offset(width * offset, size.height)
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
        }
    }
