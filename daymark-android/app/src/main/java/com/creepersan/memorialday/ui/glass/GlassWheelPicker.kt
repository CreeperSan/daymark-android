package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val WHEEL_ITEM_HEIGHT = 40.dp

/** 三行滚轮；中央行对应 firstVisibleItemIndex 指向的数值。 */
@Composable
fun GlassWheelPicker(min: Int, max: Int, state: LazyListState, modifier: Modifier = Modifier, label: (Int) -> String = { it.toString() }) {
    val scope = rememberCoroutineScope()
    // 停止滚动时吸附到最近的一行，保证中央显示的值与确认值一致。
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            val size = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            val offset = state.firstVisibleItemScrollOffset
            if (!scrolling && size > 0 && offset > 0) {
                val target = (state.firstVisibleItemIndex + if (offset * 2 >= size) 1 else 0).coerceIn(0, max - min)
                state.animateScrollToItem(target)
            }
        }
    }
    Box(modifier.height(WHEEL_ITEM_HEIGHT * 3), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(WHEEL_ITEM_HEIGHT).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)))
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(max - min + 3) { index ->
                Box(Modifier.fillMaxWidth().height(WHEEL_ITEM_HEIGHT).clickable(enabled = index in 1..(max - min + 1)) {
                    scope.launch { state.animateScrollToItem(index - 1) }
                }, contentAlignment = Alignment.Center) {
                    if (index in 1..(max - min + 1)) {
                        val selected = index == state.firstVisibleItemIndex + 1
                        Text(label(min + index - 1), color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
