package com.creepersan.memorialday.widget.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

data class CSSwitchColors(
    val checkedTrack: Color = Color.Unspecified,
    val uncheckedTrack: Color = Color.Unspecified,
    val checkedThumb: Color = Color.Unspecified,
    val uncheckedThumb: Color = Color.Unspecified
)

@Composable
fun CSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CSSwitchColors = CSSwitchColors(),
    thumbContent: (@Composable () -> Unit)? = null,
    contentDescription: String? = null
) {
    val active = enabled && onCheckedChange != null
    val checkedTrackStart = colors.checkedTrack.takeOrDefault(CSTheme.colors.primary)
    val checkedTrackEnd = if (colors.checkedTrack == Color.Unspecified) {
        CSTheme.colors.primaryStrong
    } else {
        colors.checkedTrack
    }
    val uncheckedTrackStart = colors.uncheckedTrack.takeOrDefault(CSTheme.colors.surfaceSubtle)
    val uncheckedTrackEnd = if (colors.uncheckedTrack == Color.Unspecified) {
        CSTheme.colors.border
    } else {
        colors.uncheckedTrack
    }
    val targetTrackStart = if (checked) checkedTrackStart else uncheckedTrackStart
    val targetTrackEnd = if (checked) checkedTrackEnd else uncheckedTrackEnd
    val animatedTrackStart = animateColorAsState(
        targetValue = targetTrackStart,
        animationSpec = if (CSTheme.motion.enabled) tween(durationMillis = 300) else snap(),
        label = "cs_switch_track_start"
    ).value
    val animatedTrackEnd = animateColorAsState(
        targetValue = targetTrackEnd,
        animationSpec = if (CSTheme.motion.enabled) tween(durationMillis = 300) else snap(),
        label = "cs_switch_track_end"
    ).value
    val trackBrush = Brush.horizontalGradient(listOf(animatedTrackStart, animatedTrackEnd))
    val thumbColor = if (checked) {
        colors.checkedThumb.takeOrDefault(Color.White)
    } else {
        colors.uncheckedThumb.takeOrDefault(CSTheme.colors.surface)
    }
    val endPadding = if (checked) 2.dp else 18.dp
    val thumbPadding = if (CSTheme.motion.enabled) {
        animateDpAsState(
            targetValue = endPadding,
            animationSpec = tween(durationMillis = 300),
            label = "cs_switch_thumb"
        ).value
    } else {
        endPadding
    }

    Box(
        modifier = modifier
            .csMinimumTouchTarget()
            .semantics {
                role = Role.Switch
                contentDescription?.let { this.contentDescription = it }
                stateDescription = if (checked) "已开启" else "已关闭"
            }
            .then(
                if (active) {
                    Modifier.csNoRippleClickable(role = Role.Switch) {
                        onCheckedChange?.invoke(!checked)
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackBrush)
                .alpha(if (enabled) 1f else 0.45f)
                .padding(start = thumbPadding, end = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
                contentAlignment = Alignment.Center
            ) {
                thumbContent?.invoke()
            }
        }
    }
}

private fun Color.takeOrDefault(default: Color): Color = if (this == Color.Unspecified) default else this
