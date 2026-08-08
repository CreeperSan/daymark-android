package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSNavigationBackground
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun CSNavigationBar(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    background: CSNavigationBackground = CSNavigationBackground.Solid,
    solidColor: Color = CSTheme.colors.surface,
    leftContent: @Composable () -> Unit = {},
    centerContent: @Composable () -> Unit = {},
    rightContent: @Composable () -> Unit = {}
) {
    val backgroundModifier = when (background) {
        CSNavigationBackground.Solid -> Modifier.background(solidColor)
        CSNavigationBackground.Transparent -> Modifier
        CSNavigationBackground.Gradient -> Modifier.background(
            Brush.horizontalGradient(
                listOf(CSTheme.colors.primarySoft, CSTheme.colors.surface)
            )
        )
        CSNavigationBackground.Blur -> Modifier.background(CSTheme.colors.surface.copy(alpha = 0.86f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(backgroundModifier)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                leftContent()
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                rightContent()
            }
        }
        Box(contentAlignment = Alignment.Center) {
            centerContent()
        }
    }
}
