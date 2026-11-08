package com.creepersan.memorialday.activity.setting

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.about.AboutActivity
import com.creepersan.memorialday.activity.import_legacy.ImportLegacyActivity
import com.creepersan.memorialday.activity.select_mode.SelectModeActivity
import com.creepersan.memorialday.module.user.UserManager
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
            var themeExpanded by remember { mutableStateOf(false) }
            key(model.theme) { GlassPage {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { finish() })
                        Text(getString(R.string.settings), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { model.tapTitle() }.padding(top = 5.dp))
                    }
                    if (UserManager.session() != null) {
                        Text(getString(R.string.settings_personal), style = MaterialTheme.typography.bodySmall)
                        GlassCard {
                            Text(UserManager.session()?.name.orEmpty(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                            Text(getString(R.string.logout), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                                UserManager.clear()
                                val intent = Intent(this@SettingActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                finish()
                            }.padding(vertical = 12.dp))
                        }
                    }
                    Text(getString(R.string.settings_application), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp))
                    GlassCard {
                        Row(Modifier.fillMaxWidth().clickable { themeExpanded = !themeExpanded }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getString(R.string.theme)); Text(getString(when (model.theme) { AppTheme.System -> R.string.system; AppTheme.Light -> R.string.light; AppTheme.Dark -> R.string.dark }), color = MaterialTheme.colorScheme.primary)
                        }
                        if (themeExpanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(AppTheme.System to R.string.system, AppTheme.Light to R.string.light, AppTheme.Dark to R.string.dark).forEach { (value, label) ->
                                    GlassChoice(getString(label), model.theme == value, { model.changeTheme(value) }, Modifier.weight(1f))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().clickable { languageMenu = !languageMenu }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getString(R.string.language)); Text(languages.firstOrNull { it.first == model.language }?.second ?: languages.first().second, color = MaterialTheme.colorScheme.primary)
                        }
                        if (languageMenu) languages.forEach { (value, label) ->
                            Text(label, modifier = Modifier.fillMaxWidth().clickable { languageMenu = false; model.changeLanguage(value); recreate() }.padding(vertical = 9.dp), color = if (value == model.language) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        if (UserManager.session() != null) Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@SettingActivity, ImportLegacyActivity::class.java)) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getString(R.string.import_legacy)); Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@SettingActivity, SelectModeActivity::class.java)) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getString(R.string.select_mode)); Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (model.developer) {
                        Text(getString(R.string.developer_mode), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp))
                        GlassCard {
                            Text(getString(R.string.proxy_description), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            GlassTextField(model.accountHost, { model.accountHost = it }, getString(R.string.account_server))
                            Spacer(Modifier.height(8.dp))
                            GlassTextField(model.serverHost, { model.serverHost = it }, getString(R.string.daymark_server))
                            GlassButton(getString(R.string.save), { hostMessage = if (model.saveHosts()) R.string.saved else R.string.invalid_url })
                            hostMessage?.let { Text(getString(it)) }
                            Text(getString(R.string.disable_developer), modifier = Modifier.clickable { model.disableDeveloper() }.padding(top = 12.dp))
                        }
                    }
                    Text(getString(R.string.settings_other), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp))
                    GlassCard {
                        Text(getString(R.string.app_version, com.creepersan.memorialday.BuildConfig.VERSION_NAME), modifier = Modifier.padding(vertical = 12.dp))
                        Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@SettingActivity, AboutActivity::class.java)) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getString(R.string.about_app)); Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                        listOf(R.string.agreement_terms, R.string.agreement_privacy).forEach { label ->
                            Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LEGAL_DOCUMENT_URL))) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(getString(label)); Text("›", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } }
        }
    }
}
