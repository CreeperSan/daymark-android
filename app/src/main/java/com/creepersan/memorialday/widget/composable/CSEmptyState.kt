package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSTheme

@Composable
fun CSEmptyState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .semantics { contentDescription = listOfNotNull(title, description).joinToString(": ") },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        illustration?.invoke()
        CSTextTitle(title)
        description?.let { CSTextBody(it, color = CSTheme.colors.textSecondary) }
        content?.invoke()
        action?.invoke()
    }
}
