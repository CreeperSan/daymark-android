package com.creepersan.memorialday.activity.about

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creepersan.memorialday.BuildConfig
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.ui.glass.GlassCard
import com.creepersan.memorialday.ui.glass.GlassPage

/** 应用介绍及当前版本信息。 */
class AboutActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassPage {
                Text("‹", fontSize = 32.sp, modifier = Modifier.clickable { finish() })
                Spacer(Modifier.height(24.dp))
                Box(Modifier.align(Alignment.CenterHorizontally).size(88.dp).background(Color(0xFFF28C70), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                    Text("♥", color = Color.White, fontSize = 42.sp)
                }
                Text(getString(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                Text(getString(R.string.about_description), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp))
                GlassCard {
                    Text(getString(R.string.about_story_title), fontWeight = FontWeight.SemiBold)
                    Text(getString(R.string.about_story), modifier = Modifier.padding(top = 12.dp))
                }
                GlassCard { Text(getString(R.string.app_version, BuildConfig.VERSION_NAME)) }
            }
        }
    }
}
