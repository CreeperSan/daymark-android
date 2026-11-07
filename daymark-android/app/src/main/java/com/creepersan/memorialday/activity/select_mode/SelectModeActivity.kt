package com.creepersan.memorialday.activity.select_mode

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.*
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
        fun select(mode: AppMode) { model.select(mode); startActivity(Intent(this, DispatchActivity::class.java)); finish() }
        setContent { GlassPage {
            Text(getString(R.string.select_mode), fontSize = 32.sp)
            Text(getString(R.string.select_mode_description))
            GlassCard { Text(getString(R.string.new_version), style = MaterialTheme.typography.titleLarge); Text(getString(R.string.new_description)); GlassButton(getString(R.string.open_new), { select(AppMode.Default) }) }
            GlassCard { Text(getString(R.string.classic_version), style = MaterialTheme.typography.titleLarge); Text(getString(R.string.classic_description)); GlassButton(getString(R.string.open_classic), { select(AppMode.Classic) }) }
        } }
    }
}
