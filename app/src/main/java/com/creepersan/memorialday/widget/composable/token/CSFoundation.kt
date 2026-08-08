package com.creepersan.memorialday.widget.composable.token

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CSNavigationBackground {
    Solid,
    Transparent,
    Gradient,
    Blur
}

enum class CSProgressStyle {
    Linear,
    Circular
}

enum class CSDividerStyle {
    Solid,
    Dashed,
    Label
}

enum class CSToastType {
    Success,
    Warning,
    Error,
    Info
}

enum class CSToastDeliveryPolicy {
    Queue,
    Replace
}

enum class CSToastPosition {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

enum class CSOtpInputType {
    Numeric,
    Alphabetic,
    Alphanumeric,
    Custom
}

enum class CSModalVariant {
    Dialog,
    BottomSheet
}

enum class CSModalActionArrangement {
    Horizontal,
    Vertical,
    Auto
}

enum class CSCarouselIndicatorAnchor {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

enum class CSCarouselIndicatorEdge {
    Top,
    Bottom,
    Left,
    Right
}

enum class CSCarouselIndicatorAlignment {
    Start,
    Center,
    End
}

sealed interface CSCarouselIndicatorPosition {
    @Immutable
    data class Inside(
        val anchor: CSCarouselIndicatorAnchor = CSCarouselIndicatorAnchor.BottomCenter
    ) : CSCarouselIndicatorPosition

    @Immutable
    data class Outside(
        val edge: CSCarouselIndicatorEdge = CSCarouselIndicatorEdge.Bottom,
        val alignment: CSCarouselIndicatorAlignment = CSCarouselIndicatorAlignment.Center
    ) : CSCarouselIndicatorPosition
}

enum class CSLoadingVariant {
    Spinner,
    Dots,
    Text
}

enum class CSNotificationType {
    Success,
    Warning,
    Error,
    Info
}

@Immutable
data class CSCategoryPalette(
    val background: Color,
    val accent: Color,
    val strong: Color
)

@Immutable
data class CSColors(
    val primary: Color,
    val primarySoft: Color,
    val primaryStrong: Color,
    val secondary: Color,
    val secondarySoft: Color,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val warmGradientStart: Color,
    val warmGradientEnd: Color
)

val CSColorsLight = CSColors(
    primary = Color(0xFFD9622E),
    primarySoft = Color(0xFFFDF4EE),
    primaryStrong = Color(0xFF9D4218),
    secondary = Color(0xFF4A7A51),
    secondarySoft = Color(0xFFEEF3EE),
    background = Color(0xFFFAF6F1),
    surface = Color.White,
    surfaceSubtle = Color(0xFFF2EBE3),
    textPrimary = Color(0xFF2A241D),
    textSecondary = Color(0xFF7A6F62),
    textMuted = Color(0xFF9B8E80),
    border = Color(0xFFE6DDD3),
    success = Color(0xFF4A7A51),
    warning = Color(0xFF8B5E14),
    danger = Color(0xFFB5432A),
    info = Color(0xFF3B6B85),
    warmGradientStart = Color(0xFFD9622E),
    warmGradientEnd = Color(0xFFE8A838)
)

val CSColorsDark = CSColors(
    primary = Color(0xFFFFB596),
    primarySoft = Color(0xFF5C2810),
    primaryStrong = Color(0xFFF4BD9D),
    secondary = Color(0xFFA8C2AC),
    secondarySoft = Color(0xFF1F3B23),
    background = Color(0xFF1E1915),
    surface = Color(0xFF2A241D),
    surfaceSubtle = Color(0xFF3D362E),
    textPrimary = Color(0xFFF8F0E8),
    textSecondary = Color(0xFFD0C3B5),
    textMuted = Color(0xFFB8AB9B),
    border = Color(0xFF51483E),
    success = Color(0xFFA8C2AC),
    warning = Color(0xFFE5C97D),
    danger = Color(0xFFE5A99A),
    info = Color(0xFF9BBFD4),
    warmGradientStart = Color(0xFFC0521F),
    warmGradientEnd = Color(0xFFC98A1F)
)

object CSSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val section = 24.dp
    val large = 32.dp
}

object CSTypography {
    val display = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    val sectionTitle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val cardTitle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 23.sp)
    val bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
    val hint = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
    val emphasis = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
    )
}

object CSShapes {
    val input = 10.dp
    val button = 12.dp
    val card = 16.dp
    val sheet = 20.dp
    val checkbox = 4.dp
    val pill = 999.dp
}

@Immutable
data class CSMotion(
    val enabled: Boolean = true,
    val standardMillis: Int = 250,
    val emphasizedMillis: Int = 400,
    val exitMillis: Int = 150
)

val LocalCSColors = staticCompositionLocalOf { CSColorsLight }
val LocalCSMotion = staticCompositionLocalOf { CSMotion() }

object CSTheme {
    val colors: CSColors
        @Composable get() = LocalCSColors.current

    val motion: CSMotion
        @Composable get() = LocalCSMotion.current
}

@Composable
fun CSTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (darkTheme) CSColorsDark else CSColorsLight
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.background,
            onSecondary = colors.background,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
        else -> lightColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalCSColors provides colors,
            LocalCSMotion provides CSMotion(enabled = !reducedMotion),
            content = content
        )
    }
}

fun Float.csClampedProgress(): Float = coerceIn(0f, 1f)

fun Int.csClampedIndex(size: Int): Int = if (size <= 0) 0 else coerceIn(0, size - 1)

fun Modifier.csMinimumTouchTarget(minSize: Dp = 44.dp): Modifier =
    sizeIn(minWidth = minSize, minHeight = minSize)

@Composable
fun Modifier.csNoRippleClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    return clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick
    )
}
