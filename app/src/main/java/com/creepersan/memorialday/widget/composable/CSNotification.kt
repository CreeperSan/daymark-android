package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSNotificationType
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csMinimumTouchTarget
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSNotification(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    type: CSNotificationType = CSNotificationType.Info,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    val accent = when (type) {
        CSNotificationType.Success -> CSTheme.colors.success
        CSNotificationType.Warning -> CSTheme.colors.warning
        CSNotificationType.Error -> CSTheme.colors.danger
        CSNotificationType.Info -> CSTheme.colors.info
    }
    val shape = RoundedCornerShape(CSShapes.card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CSTheme.colors.surface, shape)
            .border(1.dp, accent.copy(alpha = 0.22f), shape)
            .then(
                if (onClick != null) Modifier.csNoRippleClickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .semantics { contentDescription = listOfNotNull(title, message).joinToString(": ") }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon?.invoke()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CSTextCardTitle(title)
            message?.let { CSTextBodySmall(it) }
            action?.let { Row(content = it) }
        }
        onDismiss?.let { dismiss ->
            CSTextBody(
                text = "×",
                modifier = Modifier
                    .csMinimumTouchTarget()
                    .csNoRippleClickable(role = Role.Button, onClickLabel = "关闭", onClick = dismiss),
                color = CSTheme.colors.textSecondary
            )
        }
    }
}
