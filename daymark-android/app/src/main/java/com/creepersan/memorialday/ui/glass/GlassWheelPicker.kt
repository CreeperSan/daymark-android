package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import kotlin.math.abs

internal const val WHEEL_VISIBLE_ROWS = 7
private const val WHEEL_UNSELECTED_ALPHA = 0.55f
private const val WHEEL_HIGHLIGHT_ALPHA = 0.10f

/** 根据实际布局找到离窗口中心最近的一行，供高亮和确认共用。 */
internal fun LazyListState.centeredWheelIndex(): Int? {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
}

/** 七行滚轮，两端留白让首尾选项也能吸附在中央。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlassWheelPicker(itemCount: Int, state: LazyListState, rowHeight: Dp, modifier: Modifier = Modifier, label: (Int) -> String) {
    val scope = rememberCoroutineScope()
    val centered by remember(state) { derivedStateOf { state.centeredWheelIndex() } }
    val sidePadding = rowHeight * (WHEEL_VISIBLE_ROWS / 2)
    Box(modifier.height(rowHeight * WHEEL_VISIBLE_ROWS), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(rowHeight).background(MaterialTheme.colorScheme.primary.copy(alpha = WHEEL_HIGHLIGHT_ALPHA)))
        LazyColumn(
            modifier = Modifier.fillMaxSize().glassBounce(),
            state = state,
            contentPadding = PaddingValues(vertical = sidePadding),
            flingBehavior = rememberSnapFlingBehavior(state)
        ) {
            items(itemCount) { index ->
                val isCentered = index == centered
                Box(Modifier.fillMaxWidth().height(rowHeight).glassClickable {
                    scope.launch { state.animateScrollToItem(index) }
                }.semantics { selected = isCentered }, contentAlignment = Alignment.Center) {
                    val color = MaterialTheme.colorScheme.onSurface
                    Text(label(index), color = if (isCentered) color else color.copy(alpha = WHEEL_UNSELECTED_ALPHA), fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}
