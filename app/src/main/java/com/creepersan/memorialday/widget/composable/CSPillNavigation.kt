package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csClampedIndex
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

data class CSPillNavigationItem(
    val id: Any,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun CSPillNavigation(
    items: List<CSPillNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: (@Composable () -> Unit)? = null,
    onCenterAction: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    if (items.isEmpty()) {
        return
    }
    val normalizedIndex = selectedIndex.csClampedIndex(items.size)
    val shape = RoundedCornerShape(CSShapes.pill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 10.dp, shape = shape, clip = false)
            .clip(shape)
            .background(CSTheme.colors.surface.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp)
            .semantics { contentDescription?.let { this.contentDescription = it } },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val centerAfter = if (centerAction == null) -1 else items.size / 2
        items.forEachIndexed { index, item ->
            key(item.id) {
                if (index == centerAfter) {
                    Box(
                        modifier = Modifier
                            .csMinimumTouchTarget()
                            .clip(RoundedCornerShape(CSShapes.pill))
                            .background(CSTheme.colors.primary)
                            .then(
                                if (onCenterAction != null) {
                                    Modifier.csNoRippleClickable(role = Role.Button, onClick = onCenterAction)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        centerAction?.invoke()
                    }
                }
                val selected = index == normalizedIndex
                Column(
                    modifier = Modifier
                        .size(width = 58.dp, height = 52.dp)
                        .csMinimumTouchTarget()
                        .csNoRippleClickable(role = Role.Tab) { onItemSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    item.icon()
                    CSTextHint(
                        text = item.label,
                        color = if (selected) CSTheme.colors.primary else CSTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
