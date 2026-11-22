package com.creepersan.memorialday.activity.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.dispatch.DispatchActivity
import com.creepersan.memorialday.activity.select_mode.SelectModeActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.KV_APPLICATION_MODE
import com.creepersan.memorialday.constance.KV_WELCOME_SCREEN_SHOW_VERSION
import com.creepersan.memorialday.constance.WELCOME_SCREEN_CURRENT_VERSION
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.glass.GlassButton
import com.creepersan.memorialday.ui.glass.GlassPage
import com.creepersan.memorialday.ui.glass.glassClickable

/** 首次启动的三页引导，结束后进入模式选择。 */
class OnboardingActivity : BaseComposeActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val images = listOf(R.drawable.onboarding_record, R.drawable.onboarding_remind, R.drawable.onboarding_share)
        val titles = listOf(R.string.onboarding_record_title, R.string.onboarding_remind_title, R.string.onboarding_share_title)
        val descriptions = listOf(R.string.onboarding_record_description, R.string.onboarding_remind_description, R.string.onboarding_share_description)
        // 已选择模式的老用户直接回到启动分流，新用户继续选择模式。
        fun finishWelcome() {
            KV.set(KV_WELCOME_SCREEN_SHOW_VERSION, WELCOME_SCREEN_CURRENT_VERSION)
            val target = if (KV.has(KV_APPLICATION_MODE)) DispatchActivity::class.java else SelectModeActivity::class.java
            startActivity(Intent(this, target)); finish()
        }
        setContent {
            val pagerState = rememberPagerState()
            // 最后一页进入视野时，操作区跟随手势逐渐显示。
            val actionAlpha = (pagerState.currentPage + pagerState.currentPageOffsetFraction - (images.lastIndex - 1)).coerceIn(0f, 1f)
            val actionsEnabled = actionAlpha >= 1f
            GlassPage {
                Text(getString(R.string.onboarding_skip), color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End).alpha(1f - actionAlpha).glassClickable(enabled = actionAlpha < 1f) { finishWelcome() }.padding(8.dp))
                HorizontalPager(pageCount = images.size, state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(images[page]), contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 290.dp).weight(1f, fill = false))
                        Text(getString(titles[page]), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
                        Text(getString(descriptions[page]), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                        Spacer(Modifier.weight(1f))
                    }
                }
                Row(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    images.indices.forEach { index -> Box(Modifier.size(width = if (index == pagerState.currentPage) 16.dp else 6.dp, height = 6.dp).background(if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(3.dp))) }
                }
                Column(Modifier.alpha(actionAlpha)) {
                    GlassButton(getString(R.string.onboarding_finish), { finishWelcome() }, enabled = actionsEnabled)
                }
            }
        }
    }
}
