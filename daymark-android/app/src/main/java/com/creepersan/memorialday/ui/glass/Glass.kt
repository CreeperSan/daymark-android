package com.creepersan.memorialday.ui.glass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.creepersan.memorialday.constance.AppTheme
import com.creepersan.memorialday.constance.KV_APPLICATION_THEME
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import kotlin.random.Random

private const val GLOW_ANIMATION_DURATION_MS = 18000
private const val GLOW_ANIMATION_INTERVAL_MS = 2100
private val GlassAccent = Color(0xFFFF9459)
private val GlassAccentEnd = Color(0xFFFF6B52)

/** 根据应用设置解析页面当前使用的明暗模式。 */
@Composable
fun isGlassDark(): Boolean = when (KV.get(KV_APPLICATION_THEME)) {
    AppTheme.System -> isSystemInDarkTheme()
    AppTheme.Dark -> true
    AppTheme.Light -> false
}

/** 以渐变和缓慢移动的径向色块绘制设计稿中的柔和背景。 */
@Composable
private fun GlassBackground(dark: Boolean) {
    val transition = rememberInfiniteTransition()
    // 每次打开页面生成不同的活动范围，各色块使用不同周期缓慢往返。
    val ranges = remember { List(3) { Random.nextFloat() * 0.3f to (0.7f + Random.nextFloat() * 0.3f) } }
    val movements = ranges.mapIndexed { index, range ->
        transition.animateFloat(range.first, range.second, infiniteRepeatable(tween(GLOW_ANIMATION_DURATION_MS + index * GLOW_ANIMATION_INTERVAL_MS), RepeatMode.Reverse))
    }
    val background = if (dark) listOf(Color(0xFF201C2A), Color(0xFF38272D), Color(0xFF24212D)) else listOf(Color(0xFFFFF2E3), Color(0xFFFFDEC7), Color(0xFFFFEDE0))
    Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(background))) {
        val opacity = if (dark) 0.28f else 0.48f
        val radius = size.width * 0.58f
        val glows = listOf(
            Pair(Color(0xFFFF9E73), Offset(size.width * (0.06f + movements[0].value * 0.12f), size.height * 0.08f)),
            Pair(Color(0xFFC9A6FF), Offset(size.width * (0.88f - movements[1].value * 0.12f), size.height * 0.34f)),
            Pair(Color(0xFFFFC46B), Offset(size.width * (0.12f + movements[2].value * 0.1f), size.height * 0.84f))
        )
        glows.forEach { (color, center) ->
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = opacity), Color.Transparent), center, radius), radius = radius, center = center)
        }
    }
}

/** 新版页面统一的主题、动态背景和安全区域。 */
@Composable
fun GlassPage(horizontalPadding: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    val dark = isGlassDark()
    MemorialDayTheme(darkTheme = dark, dynamicColor = false) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Box(Modifier.fillMaxSize()) {
                GlassBackground(dark)
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = horizontalPadding, vertical = 0.dp), content = content)
            }
        }
    }
}

/** 半透明卡片，以柔和填充和描边形成玻璃质感。 */
@Composable
fun GlassCard(solid: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val dark = isGlassDark()
    val shape = RoundedCornerShape(24.dp)
    val fill = if (solid) MaterialTheme.colorScheme.surface else if (dark) Color(0xFF4B4147).copy(alpha = 0.66f) else Color.White.copy(alpha = 0.48f)
    val stroke = Color.White.copy(alpha = if (dark) 0.16f else 0.65f)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, stroke, shape).background(fill, shape).padding(20.dp), content = content)
    }
}

/** 使用设计稿暖色渐变的通用主按钮。 */
@Composable
fun GlassButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(27.dp)
    val alpha = if (enabled) 1f else 0.45f
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(54.dp).background(Brush.horizontalGradient(listOf(GlassAccent.copy(alpha = alpha), GlassAccentEnd.copy(alpha = alpha))), shape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 适用于账号和编辑表单的玻璃输入框。 */
@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, password: Boolean = false, singleLine: Boolean = true, keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    val dark = isGlassDark()
    val textColor = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(16.dp)
    var passwordVisible by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().background(Color.White.copy(alpha = if (dark) 0.08f else 0.45f), shape).border(1.dp, Color.White.copy(alpha = if (dark) 0.13f else 0.65f), shape).padding(horizontal = 16.dp, vertical = 16.dp),
        singleLine = singleLine,
        keyboardOptions = if (password && keyboardOptions == androidx.compose.foundation.text.KeyboardOptions.Default) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password) else keyboardOptions,
        textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 15.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(GlassAccent),
        visualTransformation = if (password && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = if (dark) Color(0xFFC3A99D) else Color(0xFFA5795C))
                    inner()
                }
                if (password) {
                    val eyeColor = glassSecondaryColor()
                    val description = androidx.compose.ui.res.stringResource(if (passwordVisible) com.creepersan.memorialday.R.string.hide_password else com.creepersan.memorialday.R.string.show_password)
                    Canvas(Modifier.padding(start = 10.dp).size(20.dp).clickable { passwordVisible = !passwordVisible }.then(Modifier.semantics { contentDescription = description })) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.08f, size.height * 0.5f)
                            quadraticBezierTo(size.width * 0.5f, size.height * 0.02f, size.width * 0.92f, size.height * 0.5f)
                            quadraticBezierTo(size.width * 0.5f, size.height * 0.98f, size.width * 0.08f, size.height * 0.5f)
                        }
                        drawPath(path, eyeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
                        drawCircle(eyeColor, radius = size.minDimension * 0.14f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
                    }
                }
            }
        }
    )
}

/** 可复用的选项卡片，用于日历和重复方式等单选项。 */
@Composable
fun GlassChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val fill = if (selected) Color.White.copy(alpha = if (isGlassDark()) 0.18f else 0.72f) else Color.White.copy(alpha = if (isGlassDark()) 0.08f else 0.32f)
    val stroke = if (selected) GlassAccent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f)
    Box(modifier.border(1.dp, stroke, shape).background(fill, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = if (selected) MaterialTheme.colorScheme.primary else glassSecondaryColor(), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/** 六位邮箱验证码输入框，保留单个输入焦点以支持粘贴和系统键盘。 */
@Composable
fun GlassVerificationCode(value: String, onValueChange: (String) -> Unit) {
    val codeLength = com.creepersan.memorialday.module.user.UserManager.AUTH_CODE_LENGTH
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(codeLength)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(codeLength) { index ->
                    val shape = RoundedCornerShape(14.dp)
                    Box(Modifier.weight(1f).height(58.dp).border(1.dp, if (index == value.length) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f), shape).background(Color.White.copy(alpha = if (isGlassDark()) 0.10f else 0.5f), shape), contentAlignment = Alignment.Center) {
                        Text(value.getOrNull(index)?.toString().orEmpty(), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            // 输入框由系统处理键盘和粘贴，视觉内容由上面的六个格子绘制。
            Box(Modifier.size(1.dp)) { inner() }
        }
    )
}

/** 公历与农历的分段切换控件。 */
@Composable
fun GlassSegments(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().height(44.dp).background(Color.White.copy(alpha = if (isGlassDark()) 0.08f else 0.35f), shape).border(1.dp, Color.White.copy(alpha = 0.35f), shape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val chosen = value == selected
            Box(Modifier.weight(1f).fillMaxHeight().background(if (chosen) Color.White.copy(alpha = if (isGlassDark()) 0.18f else 0.85f) else Color.Transparent, RoundedCornerShape(11.dp)).clickable { onSelect(value) }, contentAlignment = Alignment.Center) {
                Text(label, fontSize = 13.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium, color = if (chosen) MaterialTheme.colorScheme.primary else glassSecondaryColor())
            }
        }
    }
}
