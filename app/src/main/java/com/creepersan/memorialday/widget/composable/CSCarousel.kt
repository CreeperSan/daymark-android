package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorAlignment
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorAnchor
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorEdge
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorPosition
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csClampedIndex
import com.creepersan.memorialday.widget.composable.token.csElasticOverscroll
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun <T : Any> CSCarousel(
    items: List<T>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: (T) -> Any = { it },
    autoPlay: Boolean = false,
    autoPlayInterval: Long = 3_500L,
    showIndicators: Boolean = true,
    indicatorPosition: CSCarouselIndicatorPosition = CSCarouselIndicatorPosition.Outside(),
    pageSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    reducedMotion: Boolean = !CSTheme.motion.enabled,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        return
    }
    val normalizedIndex = currentIndex.csClampedIndex(items.size)
    val pagerState = rememberPagerState(
        initialPage = normalizedIndex,
        pageCount = { items.size }
    )

    LaunchedEffect(normalizedIndex, items.size) {
        if (pagerState.currentPage != normalizedIndex) {
            pagerState.animateScrollToPage(normalizedIndex)
        }
    }
    LaunchedEffect(pagerState, items.size) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onIndexChange(page.csClampedIndex(items.size))
            }
    }
    LaunchedEffect(autoPlay, reducedMotion, items.size, autoPlayInterval) {
        if (autoPlay && !reducedMotion && items.size > 1) {
            while (true) {
                delay(autoPlayInterval.coerceAtLeast(500L))
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
            }
        }
    }

    val pager: @Composable (Modifier) -> Unit = { pagerModifier ->
        HorizontalPager(
            state = pagerState,
            modifier = pagerModifier
                .fillMaxWidth()
                .csElasticOverscroll(Orientation.Horizontal),
            pageSpacing = pageSpacing,
            overscrollEffect = null,
            key = { page -> key(items[page]) }
        ) { page ->
            Box(modifier = Modifier.fillMaxWidth()) {
                itemContent(items[page])
            }
        }
    }
    val showIndicatorRow = showIndicators && items.size > 1

    if (!showIndicatorRow) {
        pager(modifier)
        return
    }

    when (indicatorPosition) {
        is CSCarouselIndicatorPosition.Inside -> {
            Box(modifier = modifier) {
                pager(Modifier.fillMaxWidth())
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(12.dp),
                    contentAlignment = indicatorPosition.anchor.toAlignment()
                ) {
                    CSCarouselIndicators(
                        itemCount = items.size,
                        selectedIndex = pagerState.settledPage,
                        onIndexChange = onIndexChange,
                        vertical = indicatorPosition.anchor.usesVerticalIndicators()
                    )
                }
            }
        }
        is CSCarouselIndicatorPosition.Outside -> {
            when (indicatorPosition.edge) {
                CSCarouselIndicatorEdge.Top,
                CSCarouselIndicatorEdge.Bottom -> {
                    Column(modifier = modifier) {
                        if (indicatorPosition.edge == CSCarouselIndicatorEdge.Top) {
                            CSCarouselIndicators(
                                itemCount = items.size,
                                selectedIndex = pagerState.settledPage,
                                onIndexChange = onIndexChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = indicatorPosition.alignment.toHorizontalArrangement()
                            )
                        }
                        pager(Modifier.fillMaxWidth())
                        if (indicatorPosition.edge == CSCarouselIndicatorEdge.Bottom) {
                            CSCarouselIndicators(
                                itemCount = items.size,
                                selectedIndex = pagerState.settledPage,
                                onIndexChange = onIndexChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = indicatorPosition.alignment.toHorizontalArrangement()
                            )
                        }
                    }
                }
                CSCarouselIndicatorEdge.Left,
                CSCarouselIndicatorEdge.Right -> {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        verticalAlignment = indicatorPosition.alignment.toVerticalAlignment()
                    ) {
                        if (indicatorPosition.edge == CSCarouselIndicatorEdge.Left) {
                            CSCarouselIndicators(
                                itemCount = items.size,
                                selectedIndex = pagerState.settledPage,
                                onIndexChange = onIndexChange,
                                vertical = true,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            pager(Modifier.fillMaxWidth())
                        }
                        if (indicatorPosition.edge == CSCarouselIndicatorEdge.Right) {
                            CSCarouselIndicators(
                                itemCount = items.size,
                                selectedIndex = pagerState.settledPage,
                                onIndexChange = onIndexChange,
                                vertical = true,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CSCarouselIndicators(
    itemCount: Int,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    vertical: Boolean = false,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val indicatorContent: @Composable () -> Unit = {
        repeat(itemCount) { index ->
            val selected = selectedIndex == index
            val selectedLength = if (selected) 20.dp else 6.dp
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = if (vertical) 0.dp else 3.dp,
                        vertical = if (vertical) 3.dp else 0.dp
                    )
                    .size(
                        width = if (vertical) 6.dp else selectedLength,
                        height = if (vertical) selectedLength else 6.dp
                    )
                    .background(
                        if (selected) CSTheme.colors.primary else CSTheme.colors.textMuted.copy(alpha = 0.35f),
                        RoundedCornerShape(3.dp)
                    )
                    .semantics {
                        role = Role.Tab
                        contentDescription = "第 ${index + 1} 页"
                    }
                    .csNoRippleClickable(role = Role.Tab) {
                        onIndexChange(index)
                    }
            )
        }
    }
    if (vertical) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            indicatorContent()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            indicatorContent()
        }
    }
}

private fun CSCarouselIndicatorAnchor.toAlignment(): Alignment = when (this) {
    CSCarouselIndicatorAnchor.TopStart -> Alignment.TopStart
    CSCarouselIndicatorAnchor.TopCenter -> Alignment.TopCenter
    CSCarouselIndicatorAnchor.TopEnd -> Alignment.TopEnd
    CSCarouselIndicatorAnchor.CenterStart -> Alignment.CenterStart
    CSCarouselIndicatorAnchor.Center -> Alignment.Center
    CSCarouselIndicatorAnchor.CenterEnd -> Alignment.CenterEnd
    CSCarouselIndicatorAnchor.BottomStart -> Alignment.BottomStart
    CSCarouselIndicatorAnchor.BottomCenter -> Alignment.BottomCenter
    CSCarouselIndicatorAnchor.BottomEnd -> Alignment.BottomEnd
}

private fun CSCarouselIndicatorAnchor.usesVerticalIndicators(): Boolean =
    this == CSCarouselIndicatorAnchor.CenterStart || this == CSCarouselIndicatorAnchor.CenterEnd

private fun CSCarouselIndicatorAlignment.toHorizontalArrangement(): Arrangement.Horizontal = when (this) {
    CSCarouselIndicatorAlignment.Start -> Arrangement.Start
    CSCarouselIndicatorAlignment.Center -> Arrangement.Center
    CSCarouselIndicatorAlignment.End -> Arrangement.End
}

private fun CSCarouselIndicatorAlignment.toVerticalAlignment(): Alignment.Vertical = when (this) {
    CSCarouselIndicatorAlignment.Start -> Alignment.Top
    CSCarouselIndicatorAlignment.Center -> Alignment.CenterVertically
    CSCarouselIndicatorAlignment.End -> Alignment.Bottom
}
