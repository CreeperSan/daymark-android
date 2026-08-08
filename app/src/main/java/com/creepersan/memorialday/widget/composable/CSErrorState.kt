package com.creepersan.memorialday.widget.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

@Composable
fun CSErrorState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    retryAction: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    CSEmptyState(
        title = title,
        description = description,
        modifier = modifier.semantics { error(description ?: title) },
        illustration = illustration,
        action = retryAction,
        content = content
    )
}
