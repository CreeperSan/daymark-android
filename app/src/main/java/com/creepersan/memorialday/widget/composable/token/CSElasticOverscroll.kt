package com.creepersan.memorialday.widget.composable.token

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

@Composable
fun Modifier.csElasticOverscroll(orientation: Orientation): Modifier {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val connection = remember(orientation) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = if (orientation == Orientation.Vertical) available.y else available.x
                if (delta != 0f) {
                    scope.launch {
                        offset.snapTo((offset.value + delta * 0.38f).coerceIn(-120f, 120f))
                    }
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                offset.animateTo(0f, spring())
                return Velocity.Zero
            }
        }
    }
    return nestedScroll(connection).graphicsLayer {
        if (orientation == Orientation.Vertical) {
            translationY = offset.value
        } else {
            translationX = offset.value
        }
    }
}
