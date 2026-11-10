package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.ScrollableState
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

// 滚动内容的留白与遮罩跟随公共标题栏高度，避免按钮放大后覆盖首项。
private val GlassHeaderContentInset = GlassNavigationBarHeight + 8.dp
private val GlassHeaderScrimHeight = GlassNavigationBarHeight + 24.dp
private val GlassFooterContentInset = 24.dp
private val GlassCardRadius = 24.dp
private val GlassTextFieldRadius = 16.dp
private val GlassAccent = Color(0xFFFF9459)
private val GlassAccentEnd = Color(0xFFFF6B52)

/** 根据应用设置解析页面当前使用的明暗模式。 */
@Composable
fun isGlassDark(): Boolean = when (KV.get(KV_APPLICATION_THEME)) {
    AppTheme.System -> isSystemInDarkTheme()
    AppTheme.Dark -> true
    AppTheme.Light -> false
}

/** 新版页面统一的主题、柔光背景和安全区域。 */
@Composable
fun GlassPage(horizontalPadding: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    val dark = isGlassDark()
    MemorialDayTheme(darkTheme = dark, dynamicColor = false) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Box(Modifier.fillMaxSize().glassBackdrop(dark)) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = horizontalPadding, vertical = 0.dp), content = content)
            }
        }
    }
}

/** 长页面在系统栏与标题栏下方滚动，按滚动位置为固定内容绘制渐隐遮罩。 */
@Composable
fun GlassScrollingPage(scrollState: ScrollableState, horizontalPadding: Dp = 20.dp, header: @Composable () -> Unit, content: @Composable BoxScope.(PaddingValues) -> Unit) {
    val dark = isGlassDark()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = bottomInset + GlassFooterContentInset
    MemorialDayTheme(darkTheme = dark, dynamicColor = false) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Box(Modifier.fillMaxSize().glassBackdrop(dark)) {
                Box(Modifier.fillMaxSize().glassScrollScrims(dark, scrollState, topInset + GlassHeaderScrimHeight, bottomPadding)) {
                    content(PaddingValues(top = topInset + GlassHeaderContentInset, bottom = bottomPadding))
                }
                Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = horizontalPadding)) { header() }
            }
        }
    }
}

/** 页面卡片使用玻璃表面，模态卡片保留不透明底色。 */
@Composable
fun GlassCard(solid: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val dark = isGlassDark()
    val surface = if (solid) {
        val shape = RoundedCornerShape(GlassCardRadius)
        val stroke = Color.White.copy(alpha = if (dark) 0.16f else 0.65f)
        Modifier.border(1.dp, stroke, shape).background(MaterialTheme.colorScheme.surface, shape)
    } else {
        Modifier.glassSurface(GlassCardRadius, dark)
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).then(surface).padding(20.dp), content = content)
    }
}

/** 使用设计稿暖色渐变的通用主按钮。 */
@Composable
fun GlassButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(27.dp)
    val alpha = if (enabled) 1f else 0.45f
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(54.dp).glassClickable(enabled = enabled, onClick = { com.creepersan.memorialday.module.log.Log.info("点击按钮：$text", "Interaction"); onClick() }).background(Brush.horizontalGradient(listOf(GlassAccent.copy(alpha = alpha), GlassAccentEnd.copy(alpha = alpha))), shape), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 适用于账号和编辑表单的玻璃输入框。 */
@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, password: Boolean = false, singleLine: Boolean = true, keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    val dark = isGlassDark()
    val textColor = MaterialTheme.colorScheme.onSurface
    var passwordVisible by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().glassSurface(GlassTextFieldRadius, dark).padding(horizontal = 16.dp, vertical = 16.dp),
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
                    Canvas(Modifier.padding(start = 10.dp).size(20.dp).glassClickable { passwordVisible = !passwordVisible }.then(Modifier.semantics { contentDescription = description })) {
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
    Box(modifier.glassClickable(onClick = onClick).border(1.dp, stroke, shape).background(fill, shape).padding(horizontal = 12.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
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
            Box(Modifier.weight(1f).fillMaxHeight().glassClickable { onSelect(value) }.background(if (chosen) Color.White.copy(alpha = if (isGlassDark()) 0.18f else 0.85f) else Color.Transparent, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text(label, fontSize = 13.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium, color = if (chosen) MaterialTheme.colorScheme.primary else glassSecondaryColor())
            }
        }
    }
}
