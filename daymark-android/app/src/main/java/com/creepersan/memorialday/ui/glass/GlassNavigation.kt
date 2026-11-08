package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** 设计稿使用的次要文字色，暗色模式单独保证可读性。 */
@Composable fun glassSecondaryColor(): Color = if (isGlassDark()) Color(0xFFD5B4A4) else Color(0xFFA5795C)

/** 与设计稿一致的圆形返回按钮和页面标题。 */
@Composable
fun GlassNavigationBar(title: String, onBack: () -> Unit, onTitleClick: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        val color = MaterialTheme.colorScheme.onBackground
        Box(Modifier.size(36.dp).border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape).background(Color.White.copy(alpha = if (isGlassDark()) 0.1f else 0.4f), CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(16.dp)) {
                drawLine(color, Offset(size.width * 0.65f, size.height * 0.18f), Offset(size.width * 0.35f, size.height * 0.5f), 1.8.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.35f, size.height * 0.5f), Offset(size.width * 0.65f, size.height * 0.82f), 1.8.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f).then(if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier), color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        action?.invoke()
    }
}

/** 不使用 Material 视觉样式的模态单选列表。 */
@Composable
fun GlassSelectionDialog(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).background(MaterialTheme.colorScheme.surface, shape).border(1.dp, Color.White.copy(alpha = 0.65f), shape).padding(20.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            val accent = MaterialTheme.colorScheme.primary
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(value) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (options.size < 5) {
                            Canvas(Modifier.size(20.dp)) {
                                drawCircle(if (selected == value) accent else Color.Gray.copy(alpha = 0.55f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                                if (selected == value) drawCircle(accent, radius = size.minDimension * 0.27f)
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (options.size >= 5 && selected == value) Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/** 账号页面中预留的第三方登录入口。 */
@Composable
fun GlassSocialOptions(label: String, onUnavailable: () -> Unit) {
    val secondary = glassSecondaryColor()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(secondary.copy(alpha = 0.25f)))
        Text(label, fontSize = 12.sp, color = secondary)
        Box(Modifier.weight(1f).height(1.dp).background(secondary.copy(alpha = 0.25f)))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        listOf(com.creepersan.memorialday.R.drawable.social_apple to com.creepersan.memorialday.R.string.social_apple, com.creepersan.memorialday.R.drawable.social_wechat to com.creepersan.memorialday.R.string.social_wechat).forEach { (icon, description) ->
            Box(Modifier.padding(horizontal = 8.dp).size(56.dp).border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape).background(Color.White.copy(alpha = if (isGlassDark()) 0.12f else 0.5f), CircleShape).clickable(onClick = onUnavailable), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(icon), contentDescription = androidx.compose.ui.res.stringResource(description), modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** 不带水波的玻璃开关，整行均可操作。 */
@Composable
fun GlassSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().height(44.dp).clickable { onCheckedChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.size(width = 44.dp, height = 26.dp).background(if (checked) accent else glassSecondaryColor().copy(alpha = 0.25f), CircleShape).padding(2.dp), contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(22.dp).background(Color.White, CircleShape))
        }
    }
}
