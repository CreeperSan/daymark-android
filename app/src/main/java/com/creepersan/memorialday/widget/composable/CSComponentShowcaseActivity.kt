package com.creepersan.memorialday.widget.composable

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.token.CSDividerStyle
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorAnchor
import com.creepersan.memorialday.widget.composable.token.CSCarouselIndicatorPosition
import com.creepersan.memorialday.widget.composable.token.CSLoadingVariant
import com.creepersan.memorialday.widget.composable.token.CSModalActionArrangement
import com.creepersan.memorialday.widget.composable.token.CSModalVariant
import com.creepersan.memorialday.widget.composable.token.CSNavigationBackground
import com.creepersan.memorialday.widget.composable.token.CSNotificationType
import com.creepersan.memorialday.widget.composable.token.CSProgressStyle
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.CSToastDeliveryPolicy
import com.creepersan.memorialday.widget.composable.token.CSToastPosition
import com.creepersan.memorialday.widget.composable.token.CSToastType

class CSComponentShowcaseActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorialDayTheme {
                CSComponentShowcaseScreen()
            }
        }
    }
}

@Composable
fun CSComponentShowcaseScreen(modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("恋爱纪念日") }
    var otp by remember { mutableStateOf("832") }
    var checked by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(true) }
    var radio by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0.68f) }
    var carouselIndex by remember { mutableIntStateOf(0) }
    var modalVisible by remember { mutableStateOf(false) }
    var navIndex by remember { mutableIntStateOf(0) }
    val toastState = CSRememberToastState()

    CSToastHost(state = toastState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CSNavigationBar(
                background = CSNavigationBackground.Gradient,
                leftContent = {
                    CSButtonText("‹", small = true, onClick = {})
                },
                centerContent = {
                    CSTextTitle("CS 组件库展示")
                },
                rightContent = {
                    CSTextHint("v2")
                }
            )

            CSShowcaseSection("01 文本") {
                CSTextDisplay("还有 96 天")
                CSTextTitle("我的纪念日")
                CSTextSectionTitle("近期纪念日")
                CSTextCardTitle("恋爱三周年")
                CSTextBody("记录并庆祝生活中的每一个重要瞬间")
                CSTextBodySmall("点击编辑纪念日信息")
                CSTextHint("每年同日提醒")
                CSTextEmphasis("96")
            }

            CSShowcaseSection("02 按钮") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CSButton("新建纪念日", onClick = {
                        toastState.show(CSToastData("按钮已点击", CSToastType.Success))
                    })
                    CSButtonSecondary("编辑", onClick = {})
                    CSButtonText("取消", onClick = {})
                }
                CSButton(
                    text = "保存",
                    fullWidth = true,
                    leadingIcon = { CSTextBody("+", color = Color.White) },
                    trailingIcon = { CSTextBody("›", color = Color.White) },
                    onClick = {}
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CSButton("小按钮", small = true, onClick = {})
                    CSButton(text = "不可用", enabled = false, onClick = {})
                    CSButtonLoading("保存中")
                }
            }

            CSShowcaseSection("03 输入框") {
                CSInput(
                    value = input,
                    onValueChange = { input = it },
                    label = "纪念日名称",
                    placeholder = "给这个日子起个名字",
                    supportingText = "好名字让你一眼认出它",
                    leadingContent = { CSTextBody("⌕", color = CSTheme.colors.textMuted) }
                )
                CSInput(
                    value = "2024.13.45",
                    onValueChange = {},
                    label = "日期（错误态）",
                    isError = true,
                    supportingText = "日期格式不正确，请检查"
                )
            }

            CSShowcaseSection("04 验证码框") {
                CSOtpInput(value = otp, onValueChange = { otp = it })
                CSTextHint("验证码已发送至 +86 138****6688")
            }

            CSShowcaseSection("05 开关") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CSTextBody("推送通知", modifier = Modifier.weight(1f))
                    CSSwitch(checked = checked, onCheckedChange = { checked = it }, contentDescription = "推送通知")
                }
            }

            CSShowcaseSection("06 单选 / 复选") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CSRadioButton(selected = radio == 0, onClick = { radio = 0 }, label = "每年")
                    CSRadioButton(selected = radio == 1, onClick = { radio = 1 }, label = "每月")
                }
                CSCheckbox(checked = selected, onCheckedChange = { selected = it }, label = "恋爱纪念日")
            }

            CSShowcaseSection("07 卡片") {
                CSCard {
                    CSTextCardTitle("恋爱纪念日")
                    CSTextEmphasis("96 天")
                    CSTextBodySmall("距离下一个周年还有 96 天")
                }
                CSCard(onClick = {}) {
                    CSTextCardTitle("北海道旅行")
                    CSTextBodySmall("12.01 · 还有 119 天")
                }
            }

            CSShowcaseSection("08 列表") {
                CSList(
                    items = listOf("恋爱纪念日", "妈妈生日", "北海道旅行"),
                    modifier = Modifier.height(160.dp),
                    key = { it },
                    header = { CSTextHint("本月安排", modifier = Modifier.padding(8.dp)) },
                    footer = { CSTextHint("共 3 项", modifier = Modifier.padding(8.dp)) },
                    onItemClick = {}
                ) { item ->
                    CSTextBody(item, modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
                CSList(
                    items = emptyList<String>(),
                    emptyContent = { CSEmptyState("暂无内容", modifier = Modifier.padding(vertical = 0.dp)) }
                ) { item -> CSTextBody(item) }
            }

            CSShowcaseSection("09 标题栏") {
                CSNavigationBar(
                    background = CSNavigationBackground.Solid,
                    leftContent = { CSTextHint("返回") },
                    centerContent = { CSTextCardTitle("标准标题") },
                    rightContent = { CSTextHint("完成") }
                )
                CSNavigationBar(
                    background = CSNavigationBackground.Transparent,
                    centerContent = { CSTextCardTitle("透明标题") }
                )
                CSNavigationBar(
                    background = CSNavigationBackground.Blur,
                    centerContent = { CSTextCardTitle("柔化标题") }
                )
            }

            CSShowcaseSection("10 进度条") {
                CSProgressBar(progress = progress, secondaryProgress = 0.86f, showPercent = true)
                CSProgressBar(
                    progress = progress,
                    style = CSProgressStyle.Circular,
                    showPercent = true,
                    completedOverlay = { CSTextBody("✓", color = CSTheme.colors.success) }
                )
                CSButtonText("增加进度", onClick = { progress = (progress + 0.1f).coerceAtMost(1f) })
            }

            CSShowcaseSection("11 分割线") {
                CSDivider()
                CSDivider(style = CSDividerStyle.Dashed)
                CSDivider(style = CSDividerStyle.Label, label = "或")
            }

            CSShowcaseSection("12 Toast") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CSButton("排队 Toast", small = true, onClick = {
                        toastState.show(
                            CSToastData(
                                message = "保存成功",
                                type = CSToastType.Success
                            )
                        )
                    })
                    CSButtonSecondary("替换 Toast", small = true, onClick = {
                        toastState.show(
                            CSToastData(
                                message = "正在同步",
                                loading = true,
                                dismissOnOutside = true,
                                position = CSToastPosition.BottomEnd
                            ),
                            CSToastDeliveryPolicy.Replace
                        )
                    })
                    CSButtonText("清除", small = true, onClick = toastState::clearAll)
                }
            }

            CSShowcaseSection("13 模态框") {
                CSButton("打开模态框", onClick = { modalVisible = true })
                CSModal(
                    visible = modalVisible,
                    onDismissRequest = { modalVisible = false },
                    variant = CSModalVariant.Dialog,
                    title = "删除纪念日？",
                    description = "删除后无法恢复，确定要删除吗？",
                    actionArrangement = CSModalActionArrangement.Auto,
                    confirmButton = {
                        CSButton("确定删除", fullWidth = true, onClick = { modalVisible = false })
                    },
                    dismissButton = {
                        CSButtonText("取消", fullWidth = true, onClick = { modalVisible = false })
                    }
                )
            }

            CSShowcaseSection("14 加载") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CSLoading()
                    CSLoading(variant = CSLoadingVariant.Dots)
                    CSLoading(variant = CSLoadingVariant.Text, label = "加载中")
                }
            }

            CSShowcaseSection("15 通知") {
                CSNotification(
                    title = "恋爱纪念日 · 3 天后",
                    message = "别忘了准备一份小礼物",
                    type = CSNotificationType.Success,
                    icon = { CSTextBody("♥", color = CSTheme.colors.success) },
                    onDismiss = {}
                )
            }

            CSShowcaseSection("16 浮岛药丸导航") {
                CSPillNavigation(
                    items = listOf(
                        CSPillNavigationItem("home", "首页") { CSTextHint("⌂") },
                        CSPillNavigationItem("calendar", "日历") { CSTextHint("□") },
                        CSPillNavigationItem("notice", "提醒") { CSTextHint("!") },
                        CSPillNavigationItem("profile", "我的") { CSTextHint("○") }
                    ),
                    selectedIndex = navIndex,
                    onItemSelected = { navIndex = it },
                    centerAction = { CSTextBody("+", color = Color.White) },
                    onCenterAction = {}
                )
            }

            CSShowcaseSection("17 骨架屏") {
                CSSkeletonCard()
                CSSkeletonCard(animated = false)
            }

            CSShowcaseSection("18 空状态") {
                CSEmptyState(
                    title = "还没有纪念日",
                    description = "点击下方按钮，记录你的第一个重要日子",
                    action = { CSButton("新建纪念日", onClick = {}) }
                )
            }

            CSShowcaseSection("19 错误状态") {
                CSErrorState(
                    title = "网络开小差了",
                    description = "请检查网络连接，稍后再试",
                    retryAction = { CSButtonSecondary("重新加载", onClick = {}) }
                )
            }

            CSShowcaseSection("20 轮播图") {
                CSCarousel(
                    items = listOf("恋爱纪念日", "北海道旅行", "入职三周年"),
                    currentIndex = carouselIndex,
                    onIndexChange = { carouselIndex = it },
                    autoPlay = true,
                    pageSpacing = 16.dp,
                    indicatorPosition = CSCarouselIndicatorPosition.Inside(
                        anchor = CSCarouselIndicatorAnchor.TopEnd
                    )
                ) { item ->
                    CSCard {
                        CSTextCardTitle(item)
                        CSTextEmphasis("96 天")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CSShowcaseSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CSTextSectionTitle(title)
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun CSComponentShowcasePreview() {
    CSTheme {
        CSComponentShowcaseScreen()
    }
}
