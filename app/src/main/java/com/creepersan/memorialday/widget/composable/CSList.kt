package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csElasticOverscroll
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun <T : Any> CSList(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: (T) -> Any = { it },
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    if (items.isEmpty()) {
        Column(modifier = modifier) {
            header?.invoke()
            emptyContent?.invoke()
            footer?.invoke()
        }
        return
    }

    LazyColumn(
        modifier = modifier.csElasticOverscroll(Orientation.Vertical),
        overscrollEffect = null
    ) {
        header?.let { item(key = "cs_list_header") { it() } }
        items(items = items, key = key) { item ->
            Box(
                modifier = if (onItemClick == null) {
                    Modifier
                } else {
                    Modifier
                        .csMinimumTouchTarget()
                        .csNoRippleClickable(role = Role.Button) { onItemClick(item) }
                }
            ) {
                itemContent(item)
            }
        }
        footer?.let { item(key = "cs_list_footer") { it() } }
    }
}
