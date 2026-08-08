package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSToastDeliveryPolicy
import com.creepersan.memorialday.widget.composable.token.CSToastPosition
import com.creepersan.memorialday.widget.composable.token.CSToastType
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos

data class CSToastData(
    val message: String,
    val type: CSToastType = CSToastType.Info,
    val durationMillis: Long = 3_000L,
    val loading: Boolean = false,
    val dismissOnOutside: Boolean = false,
    val position: CSToastPosition = CSToastPosition.Center,
    val icon: (@Composable () -> Unit)? = null,
    val action: (@Composable RowScope.() -> Unit)? = null,
    val content: (@Composable () -> Unit)? = null,
    internal val id: Long = System.nanoTime()
)

class CSToastState {
    var current by mutableStateOf<CSToastData?>(null)
        private set
    private val queue = mutableStateListOf<CSToastData>()

    fun show(
        data: CSToastData,
        policy: CSToastDeliveryPolicy = CSToastDeliveryPolicy.Queue
    ) {
        when {
            current == null -> current = data
            policy == CSToastDeliveryPolicy.Queue -> queue += data
            else -> {
                queue.clear()
                current = data
            }
        }
    }

    fun dismiss() {
        current = if (queue.isEmpty()) null else queue.removeAt(0)
    }

    fun cancel() {
        dismiss()
    }

    fun clearAll() {
        queue.clear()
        current = null
    }
}

@Composable
fun CSRememberToastState(): CSToastState = remember { CSToastState() }

@Composable
fun CSToastHost(
    state: CSToastState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val current = state.current
    var displayed by remember { mutableStateOf<CSToastData?>(null) }
    var visible by remember { mutableStateOf(false) }
    val exitMillis = CSTheme.motion.exitMillis.toLong()

    LaunchedEffect(current?.id) {
        if (current == null) {
            visible = false
            if (displayed != null) {
                delay(exitMillis)
                if (state.current == null) {
                    displayed = null
                }
            }
        } else {
            if (displayed != null && displayed?.id != current.id) {
                visible = false
                delay(exitMillis)
            }
            displayed = current
            visible = false
            withFrameNanos { }
            visible = true
        }
    }
    LaunchedEffect(current?.id) {
        if (current != null && !current.loading && current.durationMillis > 0L) {
            delay(current.durationMillis)
            if (state.current?.id == current.id) {
                state.dismiss()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        displayed?.let { toast ->
            val outsideDismiss = toast.dismissOnOutside
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (outsideDismiss) Modifier.csNoRippleClickable { state.dismiss() } else Modifier
                    ),
                contentAlignment = toast.position.toAlignment()
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .csNoRippleClickable { }
                ) {
                    CSToast(toast)
                }
            }
        }
    }
}

private fun CSToastPosition.toAlignment(): Alignment = when (this) {
    CSToastPosition.TopStart -> Alignment.TopStart
    CSToastPosition.TopCenter -> Alignment.TopCenter
    CSToastPosition.TopEnd -> Alignment.TopEnd
    CSToastPosition.CenterStart -> Alignment.CenterStart
    CSToastPosition.Center -> Alignment.Center
    CSToastPosition.CenterEnd -> Alignment.CenterEnd
    CSToastPosition.BottomStart -> Alignment.BottomStart
    CSToastPosition.BottomCenter -> Alignment.BottomCenter
    CSToastPosition.BottomEnd -> Alignment.BottomEnd
}

@Composable
fun CSToast(
    data: CSToastData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 360.dp)
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
            .semantics { contentDescription = data.message }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.icon?.invoke()
        if (data.loading) {
            CSLoading(variant = com.creepersan.memorialday.widget.composable.token.CSLoadingVariant.Spinner, size = 18.dp, color = Color.White)
        }
        if (data.content != null) {
            data.content.invoke()
        } else {
            CSTextBody(data.message, color = Color.White)
        }
        data.action?.let { Row(content = it) }
    }
}
