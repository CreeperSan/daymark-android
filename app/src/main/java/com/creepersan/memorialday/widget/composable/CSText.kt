package com.creepersan.memorialday.widget.composable

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSTypography

@Composable
private fun CSTextBase(
    text: String,
    style: TextStyle,
    modifier: Modifier,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap
    )
}

@Composable
fun CSTextDisplay(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textPrimary,
    style: TextStyle = CSTypography.display,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textPrimary,
    style: TextStyle = CSTypography.title,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textPrimary,
    style: TextStyle = CSTypography.sectionTitle,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textPrimary,
    style: TextStyle = CSTypography.cardTitle,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textPrimary,
    style: TextStyle = CSTypography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextBodySmall(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textSecondary,
    style: TextStyle = CSTypography.bodySmall,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextHint(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.textMuted,
    style: TextStyle = CSTypography.hint,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)

@Composable
fun CSTextEmphasis(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CSTheme.colors.primary,
    style: TextStyle = CSTypography.emphasis,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = false
) = CSTextBase(text, style, modifier, color, maxLines, overflow, softWrap)
