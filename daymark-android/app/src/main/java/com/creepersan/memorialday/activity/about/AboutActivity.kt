package com.creepersan.memorialday.activity.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.creepersan.memorialday.constance.LEGAL_DOCUMENT_URL
import com.creepersan.memorialday.ui.glass.*

/** 应用介绍及当前版本信息。 */
class AboutActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassPage {
                GlassNavigationBar(getString(R.string.about_app), { finish() })
                Column(Modifier.weight(1f).glassBounce().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(88.dp).background(Color.White.copy(alpha = if (isGlassDark()) 0.12f else 0.5f), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                        Text("♥", color = MaterialTheme.colorScheme.primary, fontSize = 42.sp)
                    }
                    Text(getString(R.string.app_name), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                    Text(getString(R.string.about_slogan), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp))
                    GlassCard {
                        Text(getString(R.string.about_story_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(getString(R.string.about_story), color = glassSecondaryColor(), fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    GlassCard {
                        AboutRow(getString(R.string.settings_version), BuildConfig.VERSION_NAME)
                        AboutRow(getString(R.string.about_developer), "—") { Toast.makeText(this@AboutActivity, getString(R.string.feature_developing), Toast.LENGTH_SHORT).show() }
                        AboutRow(getString(R.string.about_contact), "—") { Toast.makeText(this@AboutActivity, getString(R.string.feature_developing), Toast.LENGTH_SHORT).show() }
                    }
                    Text(getString(R.string.about_legal), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.glassClickable { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LEGAL_DOCUMENT_URL))) }.padding(14.dp))
                    Text(getString(R.string.settings_copyright), fontSize = 11.sp, color = glassSecondaryColor(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
                }
            }
        }
    }
}

/** 关于页面中的版本、开发者和联系方式行。 */
@androidx.compose.runtime.Composable
private fun AboutRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(40.dp).then(if (onClick != null) Modifier.glassClickable { onClick() } else Modifier), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(value, fontSize = 12.sp, color = glassSecondaryColor())
    }
}
