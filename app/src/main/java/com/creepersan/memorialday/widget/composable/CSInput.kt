package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSTypography
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    contentDescription: String? = label
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> CSTheme.colors.danger
        focused -> CSTheme.colors.primary
        else -> CSTheme.colors.border
    }
    val textColor = if (enabled) CSTheme.colors.textPrimary else CSTheme.colors.textMuted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                if (isError && supportingText != null) {
                    error(supportingText)
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        label?.let {
            CSTextBodySmall(
                text = it,
                color = if (isError) CSTheme.colors.danger else CSTheme.colors.textSecondary
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            textStyle = CSTypography.body.copy(color = textColor),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(CSTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .onFocusChanged { focused = it.isFocused }
                .background(CSTheme.colors.surface, RoundedCornerShape(CSShapes.input))
                .border(
                    width = if (focused || isError) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(CSShapes.input)
                ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    leadingContent?.invoke()
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            CSTextBody(
                                text = placeholder,
                                color = CSTheme.colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            }
        )
        supportingText?.let {
            CSTextHint(
                text = it,
                color = if (isError) CSTheme.colors.danger else CSTheme.colors.textMuted
            )
        }
    }
}

@Composable
fun CSPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var visible by remember { mutableStateOf(false) }
    CSInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = {
            CSTextHint(
                text = if (visible) "隐藏" else "显示",
                modifier = Modifier
                    .alpha(if (enabled) 1f else 0.5f)
                    .csNoRippleClickable(enabled = enabled) { visible = !visible },
                color = CSTheme.colors.primary
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    )
}
