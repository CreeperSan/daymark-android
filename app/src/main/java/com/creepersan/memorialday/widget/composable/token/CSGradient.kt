package com.creepersan.memorialday.widget.composable.token

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode

fun CSColors.csWarmGradient(tileMode: TileMode = TileMode.Clamp): Brush =
    Brush.linearGradient(
        colors = listOf(warmGradientStart, warmGradientEnd),
        tileMode = tileMode
    )
