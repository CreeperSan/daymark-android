package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.creepersan.memorialday.R

internal val GlassNavigationBarHeight = 56.dp
private val GlassNavigationButtonSize = 48.dp
private val GlassNavigationButtonRadius = GlassNavigationButtonSize / 2
private val GlassNavigationIconSize = 20.dp
private val GlassNavigationChevronStroke = 2.dp
private val GlassNavigationTitleGap = 12.dp
private val GlassNavigationActionPadding = 12.dp
private val GlassNavigationActionTextSize = 16.sp
private const val NAVIGATION_ACTION_DISABLED_ALPHA = 0.40f

/** 设计稿使用的次要文字色，暗色模式单独保证可读性。 */
@Composable fun glassSecondaryColor(): Color = if (isGlassDark()) Color(0xFFD5B4A4) else Color(0xFFA5795C)

/** 统一页面标题、返回入口和右侧操作。首页可以省略返回入口。 */
@Composable
fun GlassNavigationBar(title: String, onBack: (() -> Unit)? = null, onTitleClick: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(GlassNavigationBarHeight), verticalAlignment = Alignment.CenterVertically) {
        val color = MaterialTheme.colorScheme.onBackground
        if (onBack != null) {
            GlassNavigationIconButton(stringResource(R.string.navigate_back), onBack) { iconModifier ->
                Canvas(iconModifier) {
                    val stroke = GlassNavigationChevronStroke.toPx()
                    drawLine(color, Offset(size.width * 0.65f, size.height * 0.18f), Offset(size.width * 0.35f, size.height * 0.5f), stroke, StrokeCap.Round)
                    drawLine(color, Offset(size.width * 0.35f, size.height * 0.5f), Offset(size.width * 0.65f, size.height * 0.82f), stroke, StrokeCap.Round)
                }
            }
            Spacer(Modifier.width(GlassNavigationTitleGap))
        }
        Text(title, modifier = Modifier.weight(1f).then(if (onTitleClick != null) Modifier.glassClickable { onTitleClick() } else Modifier), color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (action != null) {
            Spacer(Modifier.width(GlassNavigationTitleGap))
            action()
        }
    }
}

/** 返回和图标操作共用同尺寸的玻璃按钮，读屏可直接读出操作名称。 */
@Composable
internal fun GlassNavigationIconButton(description: String, onClick: () -> Unit, icon: @Composable (Modifier) -> Unit) {
    Box(Modifier.size(GlassNavigationButtonSize).glassClickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }.glassSurface(GlassNavigationButtonRadius, isGlassDark()), contentAlignment = Alignment.Center) {
        icon(Modifier.size(GlassNavigationIconSize))
    }
}

/** 标题栏文字操作使用胶囊背景，并保留和返回按钮一致的最小触控尺寸。 */
@Composable
internal fun GlassNavigationAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = NAVIGATION_ACTION_DISABLED_ALPHA)
    Box(Modifier.sizeIn(minWidth = GlassNavigationButtonSize, minHeight = GlassNavigationButtonSize).glassClickable(enabled = enabled, role = Role.Button, onClick = onClick).glassSurface(GlassNavigationButtonRadius, isGlassDark()).padding(horizontal = GlassNavigationActionPadding), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = GlassNavigationActionTextSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Column(Modifier.glassBounce().verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).glassClickable { onSelect(value) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
            Box(Modifier.padding(horizontal = 8.dp).size(56.dp).glassClickable(onClick = onUnavailable).border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape).background(Color.White.copy(alpha = if (isGlassDark()) 0.12f else 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(icon), contentDescription = androidx.compose.ui.res.stringResource(description), modifier = Modifier.size(24.dp))
            }
        }
    }
}
