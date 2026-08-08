package com.creepersan.memorialday.widget.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.widget.composable.token.CSShapes
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSTypography
import com.creepersan.memorialday.widget.composable.token.CSOtpInputType
import com.creepersan.memorialday.widget.composable.token.csNoRippleClickable

@Composable
fun CSOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    mask: Boolean = false,
    inputType: CSOtpInputType = CSOtpInputType.Numeric,
    allowedCharacters: Set<Char> = emptySet(),
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    contentDescription: String = "验证码输入",
    slotContent: (@Composable (index: Int, value: String, focused: Boolean) -> Unit)? = null
) {
    val slotCount = length.coerceIn(1, 12)
    val normalized = value.csSanitizedOtp(inputType, allowedCharacters).take(slotCount)
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var inputValue by remember(slotCount) {
        mutableStateOf(TextFieldValue(normalized, TextRange(normalized.length)))
    }
    val focusedCell = normalized.length.coerceAtMost(slotCount - 1)

    LaunchedEffect(normalized, focused) {
        val forcedValue = TextFieldValue(normalized, TextRange(normalized.length))
        if (inputValue != forcedValue) {
            inputValue = forcedValue
        }
    }

    Box(
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
                if (isError) {
                    error("验证码不正确")
                }
            }
            .csNoRippleClickable(enabled = enabled) { focusRequester.requestFocus() }
    ) {
        DisableSelection {
            BasicTextField(
                value = inputValue,
                onValueChange = { incoming ->
                    if (enabled) {
                        val sanitized = incoming.text
                            .csSanitizedOtp(inputType, allowedCharacters)
                            .take(slotCount)
                        inputValue = TextFieldValue(sanitized, TextRange(sanitized.length))
                        onValueChange(sanitized)
                    }
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                textStyle = CSTypography.body.copy(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0.01f)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) {
                            inputValue = TextFieldValue(normalized, TextRange(normalized.length))
                        }
                    },
                decorationBox = { innerTextField -> innerTextField() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            repeat(slotCount) { index ->
                val character = normalized.getOrNull(index)?.toString().orEmpty()
                val active = focused && index == focusedCell
                val borderColor = when {
                    isError -> CSTheme.colors.danger
                    active -> CSTheme.colors.primary
                    else -> CSTheme.colors.border
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(height = 52.dp, width = 44.dp)
                        .background(CSTheme.colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(CSShapes.input))
                        .border(
                            width = if (active || isError) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(CSShapes.input)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (slotContent != null) {
                        slotContent(index, if (mask && character.isNotEmpty()) "•" else character, active)
                    } else {
                        CSTextEmphasis(
                            text = if (mask && character.isNotEmpty()) "•" else character,
                            maxLines = 1
                        )
                    }
                }

            }
        }
    }
}

private fun String.csSanitizedOtp(
    inputType: CSOtpInputType,
    allowedCharacters: Set<Char>
): String = filter { character ->
    when (inputType) {
        CSOtpInputType.Numeric -> character.isDigit()
        CSOtpInputType.Alphabetic -> character.isLetter()
        CSOtpInputType.Alphanumeric -> character.isLetterOrDigit()
        CSOtpInputType.Custom -> character in allowedCharacters
    }
}
