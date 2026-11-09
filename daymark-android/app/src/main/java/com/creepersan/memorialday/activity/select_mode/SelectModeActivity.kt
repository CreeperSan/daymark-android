package com.creepersan.memorialday.activity.select_mode

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.dispatch.DispatchActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.*
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.glass.*

/** 保存用户选择的运行模式。 */
class SelectModeViewModel : ViewModel() { fun select(mode: AppMode) = KV.set(KV_APPLICATION_MODE, mode) }

/** 首次启动时选择新版或经典版。 */
class SelectModeActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[SelectModeViewModel::class.java]
        fun select(mode: AppMode) { model.select(mode); startActivity(Intent(this, DispatchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish() }
        setContent { GlassPage {
            GlassNavigationBar("", { finish() })
            Spacer(Modifier.height(16.dp))
            Text(getString(R.string.select_mode), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(getString(R.string.select_mode_description), modifier = Modifier.padding(top = 10.dp, bottom = 24.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            GlassCard {
                Text(getString(R.string.new_version), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(getString(R.string.new_description), modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(getString(R.string.mode_features), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            GlassCard {
                Text(getString(R.string.classic_version), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(getString(R.string.classic_description), modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(Modifier.weight(1f))
            GlassButton(getString(R.string.open_new), { select(AppMode.Default) })
            Box(Modifier.fillMaxWidth().glassClickable { select(AppMode.Classic) }.padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(getString(R.string.open_classic), color = MaterialTheme.colorScheme.primary)
            }
        } }
    }
}
