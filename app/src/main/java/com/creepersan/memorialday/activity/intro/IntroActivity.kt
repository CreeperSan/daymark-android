package com.creepersan.memorialday.activity.intro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.creepersan.memorialday.R
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.module.route.navigation.CSNavigation
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSCheckbox
import com.creepersan.memorialday.widget.composable.token.CSTheme
import com.creepersan.memorialday.widget.composable.token.csElasticOverscroll
import kotlinx.coroutines.launch

class IntroActivity : BaseComposeActivity() {

    private val mViewModel: IntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by mViewModel.uiStateFlow.collectAsStateWithLifecycle()

            BuildPage(
                uiState = uiState,
            )
        }
    }


    @Composable
    private fun BuildPage(
        uiState: IntroUIState,
    ) {
        val statePagerData = listOf(
            IntroPagerData(
                iconRes = R.drawable.ic_launcher_background,
                title = "开始你的纪念之旅",
                description = "每一个值得铭记的瞬间，都值得被温柔以待",
            ),
            IntroPagerData(
                iconRes = R.drawable.ic_launcher_background,
                title = "记录每一个重要日子",
                description = "恋爱纪念日、生日、入职周年……\n所有值得铭记的瞬间，都在这里被温柔收藏",
            ),
            IntroPagerData(
                iconRes = R.drawable.ic_launcher_background,
                title = "分类管理 一目了然",
                description = "恋爱、生日、旅行、里程碑……\n每种日子都有自己的色彩性格，翻阅如同一本多彩的日记",
            ),
        )

        val statePagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { statePagerData.size },
        )

        CSTheme {
            Scaffold { insets ->
                Column() {
                    Spacer(
                        modifier = Modifier.height(insets.calculateTopPadding())
                    )
                    HorizontalPager(
                        state = statePagerState,
                        modifier = Modifier.weight(1f).csElasticOverscroll(Orientation.Horizontal),
                        overscrollEffect = null,
                    ) { index ->
                        val pagerData = statePagerData[index]

                        Column(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(pagerData.iconRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(92.dp).height(92.dp),
                            )
                            Spacer(
                                modifier = Modifier.height(42.dp)
                            )
                            Text(
                                text = pagerData.title,
                                color = Color(0xFF000000),
                                fontSize = 22.sp,
                                fontWeight = FontWeight(500),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                            Text(
                                text = pagerData.description,
                                color = Color(0xFF666666),
                                fontSize = 16.sp,
                                fontWeight = FontWeight(300),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CSButton(
                            text = "开始使用",
                            modifier = Modifier.requiredWidth(300.dp),
                            onClick = { CSNavigation.replaceToLoginActivity(this@IntroActivity) }
                        )
                        CSCheckbox(
                            enabled = true,
                            checked = uiState.hasAgreeServiceAgreement,
                            label = "我已同意服务协议",
                            onCheckedChange = mViewModel::onToggleAgreeServiceAgreement,
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(insets.calculateBottomPadding())
                    )
                }
            }
        }
    }


    /* 首页 Pager */
    private data class IntroPagerData (
        val iconRes: Int,
        val title: String,
        val description: String,
    )

}