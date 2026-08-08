package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSModalActionArrangement
import com.creepersan.memorialday.widget.composable.token.CSModalVariant
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSModal(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CSModalVariant = CSModalVariant.Dialog,
    title: String? = null,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    actionArrangement: CSModalActionArrangement = CSModalActionArrangement.Auto,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = {
            if (dismissOnBackPress || dismissOnClickOutside) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .then(
                    if (dismissOnClickOutside) Modifier.csNoRippleClickable(onClick = onDismissRequest) else Modifier
                ),
            contentAlignment = if (variant == CSModalVariant.Dialog) Alignment.Center else Alignment.BottomCenter
        ) {
            val shape = if (variant == CSModalVariant.Dialog) {
                RoundedCornerShape(CSShapes.sheet)
            } else {
                RoundedCornerShape(topStart = CSShapes.sheet, topEnd = CSShapes.sheet)
            }
            Column(
                modifier = modifier
                    .fillMaxWidth(if (variant == CSModalVariant.Dialog) 0.9f else 1f)
                    .widthIn(max = 480.dp)
                    .clip(shape)
                    .background(CSTheme.colors.surface)
                    .csNoRippleClickable { }
                    .padding(24.dp),
                horizontalAlignment = if (variant == CSModalVariant.Dialog) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (variant == CSModalVariant.BottomSheet) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.12f)
                            .background(CSTheme.colors.border, RoundedCornerShape(2.dp))
                            .padding(vertical = 2.dp)
                    )
                }
                icon?.invoke()
                title?.let { CSTextTitle(it) }
                description?.let { CSTextBody(it, color = CSTheme.colors.textSecondary) }
                content?.invoke()
                if (confirmButton != null || dismissButton != null) {
                    CSModalActions(
                        arrangement = actionArrangement,
                        confirmButton = confirmButton,
                        dismissButton = dismissButton
                    )
                }
            }
        }
    }
}

@Composable
private fun CSModalActions(
    arrangement: CSModalActionArrangement,
    confirmButton: (@Composable () -> Unit)?,
    dismissButton: (@Composable () -> Unit)?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val resolvedArrangement = when (arrangement) {
            CSModalActionArrangement.Horizontal -> CSModalActionArrangement.Horizontal
            CSModalActionArrangement.Vertical -> CSModalActionArrangement.Vertical
            CSModalActionArrangement.Auto -> {
                if (maxWidth >= 240.dp) {
                    CSModalActionArrangement.Horizontal
                } else {
                    CSModalActionArrangement.Vertical
                }
            }
        }
        if (resolvedArrangement == CSModalActionArrangement.Horizontal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                confirmButton?.let { button ->
                    Box(modifier = Modifier.weight(1f)) {
                        button()
                    }
                }
                dismissButton?.let { button ->
                    Box(modifier = Modifier.weight(1f)) {
                        button()
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                confirmButton?.invoke()
                dismissButton?.invoke()
            }
        }
    }
}
