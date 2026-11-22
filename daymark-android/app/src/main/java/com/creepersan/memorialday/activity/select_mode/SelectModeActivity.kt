package com.creepersan.memorialday.activity.select_mode

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.dispatch.DispatchActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.*
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.glass.*

private val ModePagePadding = 24.dp
private val ModeNavigationPadding = 16.dp
private val ModeNavigationTop = 62.dp
private val ModeNavigationHeight = 52.dp
private val ModeBackTouchSize = 48.dp
private val ModeBackCircleSize = 36.dp
private val ModeSectionSpacing = 16.dp
private val ModeHeaderTopPadding = 8.dp
private val ModeBackdropStart = Color(0xFFFFF5EB)
private val ModeBackdropMiddle = Color(0xFFFFDEC9)
private val ModeBackdropEnd = Color(0xFFFFF0E3)
private val ModeAmberGlow = Color(0x73FFB559)
private val ModeRoseGlow = Color(0x6BFF9470)
private val ModeAmberCenter = Offset(100f / 393f, 110f / 852f)
private val ModeRoseCenter = Offset(340f / 393f, 600f / 852f)
private const val MODE_GLOW_RADIUS_RATIO = 180f / 393f
private val ModeHeadingColor = Color(0xFF4A2511)
private val ModeSecondaryColor = Color(0xFF8A5A3C)
private val ModeNewDescriptionColor = Color(0xFF6B3A1F)
private val ModeAccentStart = Color(0xFFFF9459)
private val ModeAccentEnd = Color(0xFFFF6B52)
private val ModeClassicOutline = Color(0x8CE0A67A)
private val ModeClassicTileColor = Color(0xFFFFEDDE)
private val ModeClassicTileOutline = Color(0xCCF0C7A6)
private val ModeClassicChevronColor = Color(0xFFC08B63)
private val ModeFeatureColor = Color(0xFFA9612F)
private val ModeIconColor = Color(0xFFC0703C)
private val ModeAccentBrush = Brush.horizontalGradient(listOf(ModeAccentStart, ModeAccentEnd))
private val ModeNewShadow = Color(0x3DFF734D)
private val ModeClassicShadow = Color(0x1A995C2E)
private val ModeIconShadow = Color(0x66FF6B47)

/** 保存用户选择的运行模式。 */
class SelectModeViewModel : ViewModel() { fun select(mode: AppMode) = KV.set(KV_APPLICATION_MODE, mode) }

/** 首次启动或从设置进入时，点击卡片直接切换使用模式。 */
@OptIn(ExperimentalLayoutApi::class)
class SelectModeActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[SelectModeViewModel::class.java]
        fun select(mode: AppMode) { model.select(mode); startActivity(Intent(this, DispatchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish() }
        setContent {
            val dark = isGlassDark()
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            GlassPage(horizontalPadding = 0.dp, backdrop = if (dark) null else Modifier.modeBackdrop()) {
                // 系统状态栏由 Android 管理，补齐设计稿顶部留白并保留更高状态栏的空间。
                Spacer(Modifier.height((ModeNavigationTop - topInset).coerceAtLeast(0.dp)))
                ModeNavigation(dark, onBack = { finish() })
                // 标题和卡片随内容滚动，返回按钮固定在系统状态栏下方。
                Column(Modifier.fillMaxWidth().glassBounce().verticalScroll(rememberScrollState()).padding(start = ModePagePadding, end = ModePagePadding, top = ModeHeaderTopPadding, bottom = 32.dp)) {
                    val heading = if (dark) MaterialTheme.colorScheme.onBackground else ModeHeadingColor
                    val secondary = if (dark) glassSecondaryColor() else ModeSecondaryColor
                    Text(stringResource(R.string.select_mode), fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = heading)
                    Spacer(Modifier.height(ModeSectionSpacing))
                    Text(stringResource(R.string.select_mode_description), fontSize = 14.sp, lineHeight = 21.sp, color = secondary)
                    Spacer(Modifier.height(ModeSectionSpacing * 2 + ModeHeaderTopPadding))
                    ModeCard(recommended = true, heading = heading, dark = dark, onClick = { select(AppMode.Default) }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.mode_new_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = heading)
                            Box(Modifier.heightIn(min = 22.dp).background(ModeAccentBrush, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.mode_recommended), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                        Text(stringResource(R.string.new_description), fontSize = 13.sp, lineHeight = 20.sp, color = if (dark) secondary else ModeNewDescriptionColor)
                        // 窄屏和较长的译文可以换行，标签仍保留在新版卡片内。
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ModeFeature(R.string.mode_features, R.drawable.ic_mode_cloud, dark)
                            ModeFeature(R.string.mode_new_interface, R.drawable.ic_mode_interface, dark)
                        }
                    }
                    Spacer(Modifier.height(ModeSectionSpacing))
                    ModeCard(recommended = false, heading = heading, dark = dark, onClick = { select(AppMode.Classic) }) {
                        Text(stringResource(R.string.mode_classic_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = heading)
                        Text(stringResource(R.string.classic_description), fontSize = 12.sp, lineHeight = 19.sp, color = secondary)
                    }
                }
            }
        }
    }
}

/** 渲染两种模式共用的卡片结构与各自的描边、图标和箭头。 */
@Composable
private fun ModeCard(recommended: Boolean, heading: Color, dark: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val radius = if (recommended) 24.dp else 22.dp
    val shape = RoundedCornerShape(radius)
    val fill = Color.White.copy(alpha = if (dark) 0.08f else if (recommended) 0.50f else 0.62f)
    val outline = if (recommended) ModeAccentEnd.copy(alpha = 0.7f) else if (dark) Color.White.copy(alpha = 0.2f) else ModeClassicOutline
    val shadowColor = if (recommended) ModeNewShadow else ModeClassicShadow
    val chevronColor = if (recommended) ModeAccentEnd else if (dark) heading else ModeClassicChevronColor
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (recommended) 134.dp else 106.dp).glassClickable(role = Role.Button, onClick = onClick)
            .modeShadow(radius, if (recommended) 32.dp else 20.dp, if (recommended) 14.dp else 8.dp, shadowColor)
            .background(fill, shape).border(1.dp, outline, shape)
            .padding(start = if (recommended) 20.dp else 18.dp, end = 18.dp, top = if (recommended) 24.dp else 20.dp, bottom = if (recommended) 24.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(if (recommended) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeIconTile(recommended, dark)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (recommended) 8.dp else 5.dp), content = content)
        Canvas(Modifier.size(18.dp)) {
            val tip = Offset(11.8.dp.toPx(), 9.dp.toPx())
            val stroke = 1.8.dp.toPx()
            drawLine(chevronColor, Offset(7.dp.toPx(), 4.2.dp.toPx()), tip, stroke, StrokeCap.Round)
            drawLine(chevronColor, tip, Offset(7.dp.toPx(), 13.8.dp.toPx()), stroke, StrokeCap.Round)
        }
    }
}

/** 新版使用渐变星光图标，经典版使用浅色笔记本图标。 */
@Composable
private fun ModeIconTile(recommended: Boolean, dark: Boolean) {
    val radius = if (recommended) 16.dp else 14.dp
    val shape = RoundedCornerShape(radius)
    val tile = if (recommended) {
        Modifier.modeShadow(radius, 12.dp, 6.dp, ModeIconShadow).background(ModeAccentBrush, shape)
    } else {
        Modifier.background(if (dark) Color.White.copy(alpha = 0.10f) else ModeClassicTileColor, shape).border(1.dp, ModeClassicTileOutline, shape)
    }
    Box(Modifier.size(if (recommended) 48.dp else 44.dp).then(tile), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(if (recommended) R.drawable.ic_mode_sparkles else R.drawable.ic_mode_notebook),
            contentDescription = null,
            modifier = Modifier.size(if (recommended) 24.dp else 22.dp),
            tint = if (recommended) Color.White else if (dark) glassSecondaryColor() else ModeIconColor
        )
    }
}

/** 新版功能标签，仅作为卡片说明。 */
@Composable
private fun ModeFeature(@StringRes label: Int, @DrawableRes icon: Int, dark: Boolean) {
    val fill = Color.White.copy(alpha = if (dark) 0.12f else 0.60f)
    Row(Modifier.background(fill, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(12.dp), tint = if (dark) glassSecondaryColor() else ModeIconColor)
        Text(stringResource(label), fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = if (dark) glassSecondaryColor() else ModeFeatureColor)
    }
}

/** 圆形返回按钮按设计稿绘制，独立保留 48dp 的点击区域。 */
@Composable
private fun ModeNavigation(dark: Boolean, onBack: () -> Unit) {
    val label = stringResource(R.string.navigate_back)
    val iconColor = if (dark) MaterialTheme.colorScheme.onBackground else ModeHeadingColor
    val fill = Color.White.copy(alpha = if (dark) 0.12f else 0.50f)
    val outline = Color.White.copy(alpha = if (dark) 0.25f else 0.70f)
    Box(Modifier.fillMaxWidth().height(ModeNavigationHeight).padding(horizontal = ModeNavigationPadding - (ModeBackTouchSize - ModeBackCircleSize) / 2), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.size(ModeBackTouchSize).glassClickable(role = Role.Button, onClick = onBack).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(ModeBackCircleSize).background(fill, CircleShape).border(1.dp, outline, CircleShape), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(16.dp)) {
                    val tip = Offset(5.dp.toPx(), 8.dp.toPx())
                    val stroke = 1.8.dp.toPx()
                    drawLine(iconColor, Offset(10.dp.toPx(), 3.dp.toPx()), tip, stroke, StrokeCap.Round)
                    drawLine(iconColor, tip, Offset(10.dp.toPx(), 13.dp.toPx()), stroke, StrokeCap.Round)
                }
            }
        }
    }
}

/** 仅在圆角轮廓外绘制阴影，玻璃卡片内部保留连续、均匀的透色。 */
private fun Modifier.modeShadow(radius: Dp, blur: Dp, offsetY: Dp, color: Color): Modifier = drawWithCache {
    val corner = radius.toPx()
    val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner))) }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        setShadowLayer(blur.toPx() / 2f, 0f, offsetY.toPx(), color.toArgb())
    }
    onDrawBehind {
        clipPath(outline, ClipOp.Difference) {
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint) }
        }
    }
}

/** 将设计稿的两处柔光按屏幕比例布置在浅色背景上。 */
private fun Modifier.modeBackdrop(): Modifier = drawWithCache {
    val base = Brush.verticalGradient(0f to ModeBackdropStart, 0.55f to ModeBackdropMiddle, 1f to ModeBackdropEnd)
    val radius = size.width * MODE_GLOW_RADIUS_RATIO
    if (radius <= 0f) return@drawWithCache onDrawBehind { }
    val amber = Brush.radialGradient(listOf(ModeAmberGlow, Color.Transparent), Offset(size.width * ModeAmberCenter.x, size.height * ModeAmberCenter.y), radius)
    val rose = Brush.radialGradient(listOf(ModeRoseGlow, Color.Transparent), Offset(size.width * ModeRoseCenter.x, size.height * ModeRoseCenter.y), radius)
    onDrawBehind {
        drawRect(base)
        drawRect(amber)
        drawRect(rose)
    }
}
