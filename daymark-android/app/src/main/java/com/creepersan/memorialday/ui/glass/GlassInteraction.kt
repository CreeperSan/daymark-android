package com.creepersan.memorialday.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PRESSED_ALPHA = 0.56f
private const val PRESS_DURATION_MS = 70
private const val RELEASE_DURATION_MS = 180
private val BounceLimit = 76.dp
private const val BOUNCE_DRAG_FACTOR = 0.55f
private const val BOUNCE_FLING_RESISTANCE = 32f
private const val BOUNCE_FLING_LIMIT = 0.45f

/** 可点击内容的统一淡化反馈。置于背景等绘制修饰符之前可淡化整个控件。 */
fun Modifier.glassClickable(enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null, onClick: () -> Unit): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    val opacity = remember { Animatable(1f) }
    LaunchedEffect(interactions, enabled) {
        opacity.snapTo(1f)
        if (enabled) interactions.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> opacity.animateTo(PRESSED_ALPHA, tween(PRESS_DURATION_MS))
                is PressInteraction.Release -> {
                    // 滚动容器里的短点按可能在同一帧发出按下和抬起，也要完整显示反馈。
                    if (opacity.value > PRESSED_ALPHA) opacity.animateTo(PRESSED_ALPHA, tween(PRESS_DURATION_MS))
                    opacity.animateTo(1f, tween(RELEASE_DURATION_MS))
                }
                is PressInteraction.Cancel -> opacity.animateTo(1f, tween(RELEASE_DURATION_MS))
            }
        }
    }
    graphicsLayer { alpha = opacity.value }.clickable(interactionSource = interactions, indication = null, enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
}

/** 加在滚动修饰符之前或 LazyColumn 的 modifier 上，只位移该滚动容器。 */
fun Modifier.glassBounce(orientation: Orientation = Orientation.Vertical): Modifier = composed {
    val scope = rememberCoroutineScope()
    val limit = with(LocalDensity.current) { BounceLimit.toPx() }
    val connection = remember(scope, limit, orientation) { GlassBounceConnection(scope, limit, orientation) }
    clipToBounds().nestedScroll(connection).graphicsLayer {
        if (orientation == Orientation.Vertical) translationY = connection.distance.value
        else translationX = connection.distance.value
    }
}

/** 仅在列表用完可滚动距离后产生位移，反向拖动优先收回位移。 */
private class GlassBounceConnection(private val scope: CoroutineScope, private val limit: Float, private val orientation: Orientation) : NestedScrollConnection {
    val distance = mutableStateOf(0f)
    private var rebound: Job? = null

    /** 将指针位移投影到滚动轴，避免横向手势影响纵向回弹。 */
    private fun axis(offset: Offset): Float = if (orientation == Orientation.Vertical) offset.y else offset.x

    /** 返回滚动轴上已消费的距离。 */
    private fun alongAxis(value: Float): Offset = if (orientation == Orientation.Vertical) Offset(0f, value) else Offset(value, 0f)

    /** 反向拖动先把内容拉回原位，剩余距离再交给列表滚动。 */
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = axis(available)
        if (source != NestedScrollSource.Drag || delta == 0f || distance.value == 0f) return Offset.Zero
        rebound?.cancel()
        if (delta * distance.value >= 0f) return Offset.Zero
        val consumed = if (abs(delta) > abs(distance.value)) -distance.value else delta
        distance.value += consumed
        return alongAxis(consumed)
    }

    /** 边缘拖得越远阻力越大，内容位移限制在固定范围内。 */
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val delta = axis(available)
        if (source != NestedScrollSource.Drag || delta == 0f) return Offset.Zero
        rebound?.cancel()
        val resistance = 1f - abs(distance.value) / limit
        distance.value = (distance.value + delta * BOUNCE_DRAG_FACTOR * resistance).coerceIn(-limit, limit)
        return alongAxis(delta)
    }

    /** 松手或惯性滚动触边后，用阻尼弹簧把内容带回原位。 */
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val velocity = if (orientation == Orientation.Vertical) available.y else available.x
        if (distance.value == 0f) {
            val flingLimit = limit * BOUNCE_FLING_LIMIT
            distance.value = (velocity / BOUNCE_FLING_RESISTANCE).coerceIn(-flingLimit, flingLimit)
        }
        if (distance.value == 0f) return Velocity.Zero
        rebound?.cancel()
        rebound = scope.launch {
            animate(distance.value, 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                distance.value = value
            }
        }
        return if (orientation == Orientation.Vertical) Velocity(0f, velocity) else Velocity(velocity, 0f)
    }
}
