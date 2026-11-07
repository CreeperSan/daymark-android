package com.creepersan.memorialday.activity.setting

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.*
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.ui.glass.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val DEVELOPER_TAP_COUNT = 8
private const val DEVELOPER_TAP_INTERVAL_MS = 500L

/** 保存新版设置，开发者模式通过连续点击标题解锁。 */
class SettingViewModel : ViewModel() {
    var theme by mutableStateOf(KV.get(KV_APPLICATION_THEME)); private set
    var language by mutableStateOf(KV.get(KV_APPLICATION_LANGUAGE)); private set
    var developer by mutableStateOf(KV.get(KV_APPLICATION_DEV_MODE)); private set
    var accountHost by mutableStateOf(KV.get(KV_APPLICATION_PROXY_ACCOUNT))
    var serverHost by mutableStateOf(KV.get(KV_APPLICATION_PROXY_SERVER))
    private var taps = 0
    private var lastTap = 0L
    fun changeTheme(value: AppTheme) { theme = value; KV.set(KV_APPLICATION_THEME, value) }
    fun changeLanguage(value: String) { language = value; KV.set(KV_APPLICATION_LANGUAGE, value) }
    fun tapTitle() {
        val now = SystemClock.elapsedRealtime()
        taps = if (now - lastTap <= DEVELOPER_TAP_INTERVAL_MS) taps + 1 else 1
        lastTap = now
        if (taps >= DEVELOPER_TAP_COUNT) { developer = true; KV.set(KV_APPLICATION_DEV_MODE, true); taps = 0 }
    }
    fun disableDeveloper() { developer = false; KV.set(KV_APPLICATION_DEV_MODE, false) }
    fun saveHosts(): Boolean {
        val account = accountHost.trim().trimEnd('/'); val server = serverHost.trim().trimEnd('/')
        if (listOf(account, server).any { it.isNotEmpty() && it.toHttpUrlOrNull() == null }) return false
        KV.set(KV_APPLICATION_PROXY_ACCOUNT, account); KV.set(KV_APPLICATION_PROXY_SERVER, server)
        accountHost = account; serverHost = server
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/** 主题、语言和开发者代理设置。 */
class SettingActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[SettingViewModel::class.java]
        setContent {
            var languageMenu by remember { mutableStateOf(false) }
            var hostMessage by remember { mutableStateOf<Int?>(null) }
            val languages = listOf("system" to getString(R.string.system), "zh-CN" to "简体中文（中国）", "zh-TW" to "繁體中文（台灣）", "yue-Hans-CN" to "简体粤语（广东）", "yue-Hant-HK" to "繁體粵語（香港）", "en" to "English", "ja" to "日本語", "ko" to "한국어")
            key(model.theme) { GlassPage {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(getString(R.string.settings), fontSize = 32.sp, modifier = Modifier.clickable { model.tapTitle() })
                    GlassCard {
                        Text(getString(R.string.theme), style = MaterialTheme.typography.titleMedium)
                        listOf(AppTheme.System to R.string.system, AppTheme.Light to R.string.light, AppTheme.Dark to R.string.dark).forEach { (value, label) ->
                            Row(Modifier.fillMaxWidth().clickable { model.changeTheme(value) }) { RadioButton(selected = model.theme == value, onClick = { model.changeTheme(value) }); Text(getString(label), modifier = Modifier.padding(top = 12.dp)) }
                        }
                    }
                    GlassCard {
                        Text(getString(R.string.language), style = MaterialTheme.typography.titleMedium)
                        Box { TextButton(onClick = { languageMenu = true }) { Text(languages.firstOrNull { it.first == model.language }?.second ?: languages.first().second) }
                            DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                                languages.forEach { (value, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { languageMenu = false; model.changeLanguage(value); recreate() }) }
                            }
                        }
                    }
                    if (model.developer) GlassCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(getString(R.string.developer_mode)); Switch(checked = true, onCheckedChange = { if (!it) model.disableDeveloper() }) }
                        Text(getString(R.string.proxy_description), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(model.accountHost, { model.accountHost = it }, label = { Text(getString(R.string.account_server)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(model.serverHost, { model.serverHost = it }, label = { Text(getString(R.string.daymark_server)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        GlassButton(getString(R.string.save), { hostMessage = if (model.saveHosts()) R.string.saved else R.string.invalid_url })
                        hostMessage?.let { Text(getString(it)) }
                    }
                }
            } }
        }
    }
}
