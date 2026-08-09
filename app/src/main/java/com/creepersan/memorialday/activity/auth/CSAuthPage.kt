package com.creepersan.memorialday.activity.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.CSDivider
import com.creepersan.memorialday.widget.composable.CSTextBody
import com.creepersan.memorialday.widget.composable.CSTextHint
import com.creepersan.memorialday.widget.composable.CSTextTitle
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSDividerStyle
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSAuthPage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CSTheme.colors.background, CSTheme.colors.surfaceSubtle)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 32.dp, vertical = 48.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBack) {
            CSTextHint(
                text = "返回",
                modifier = Modifier
                    .csNoRippleClickable(onClick = { onBack?.invoke() })
                    .semantics { contentDescription = "返回上一页" },
                color = CSTheme.colors.primary
            )
        }
        CSAuthLogo()
        CSTextTitle(title)
        CSTextBody(subtitle, color = CSTheme.colors.textSecondary)
        content()
        footer?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                it()
            }
        }
    }
}

@Composable
private fun CSAuthLogo() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                CSTheme.colors.primary.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp)
            )
            .semantics { contentDescription = "Daymark" },
        contentAlignment = Alignment.Center
    ) {
        CSTextTitle("✓", color = CSTheme.colors.primary)
    }
}

@Composable
fun CSAuthDivider() {
    CSDivider(style = CSDividerStyle.Label, label = "或")
}

@Composable
fun CSAuthUnavailableSocialLogin() {
    CSTextHint(
        text = "第三方登录暂未开放",
        modifier = Modifier.fillMaxWidth(),
        color = Color.Gray
    )
}
