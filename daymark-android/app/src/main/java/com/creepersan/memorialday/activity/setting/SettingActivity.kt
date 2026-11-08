package com.creepersan.memorialday.activity.setting

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** 设置页；标题栏固定，设置项通过模态弹窗编辑。 */
class SettingActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[SettingViewModel::class.java]
        setContent {
            var dialog by remember { mutableStateOf<String?>(null) }
            var hostMessage by remember { mutableStateOf<Int?>(null) }
            val languages = listOf("system" to getString(R.string.system), "zh-CN" to "简体中文（中国）", "zh-TW" to "繁體中文（台灣）", "yue-Hans-CN" to "简体粤语（广东）", "yue-Hant-HK" to "繁體粵語（香港）", "en" to "English", "ja" to "日本語", "ko" to "한국어")
            val themes = listOf(AppTheme.System.name to getString(R.string.system), AppTheme.Light.name to getString(R.string.light), AppTheme.Dark.name to getString(R.string.dark))
            fun developing() = android.widget.Toast.makeText(this@SettingActivity, getString(R.string.feature_developing), android.widget.Toast.LENGTH_SHORT).show()
            fun developerLocked() = android.widget.Toast.makeText(this@SettingActivity, getString(R.string.developer_locked), android.widget.Toast.LENGTH_SHORT).show()
            key(model.theme) { GlassPage(horizontalPadding = 20.dp) {
                GlassNavigationBar(getString(R.string.settings), { finish() }, { model.tapTitle() })
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSection(getString(R.string.settings_personal)) {
                        SettingsRow(getString(R.string.settings_avatar), onClick = { developing() })
                        SettingsRow(getString(R.string.settings_username), UserManager.session()?.name.orEmpty().ifBlank { "—" }, onClick = { developing() })
                        SettingsRow(getString(R.string.account_email), "—", onClick = { developing() })
                        SettingsRow(getString(R.string.settings_change_password), onClick = { developing() })
                        SettingsRow(getString(R.string.settings_delete_account), danger = true, onClick = { developing() })
                        SettingsRow(getString(R.string.logout), danger = true, centered = true, onClick = {
                            if (UserManager.session() == null) { developing() } else {
                                UserManager.clear()
                                startActivity(Intent(this@SettingActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                finish()
                            }
                        })
                    }
                    SettingsSection(getString(R.string.settings_application)) {
                        SettingsRow(getString(R.string.theme), themes.first { it.first == model.theme.name }.second, onClick = { dialog = "theme" })
                        SettingsRow(getString(R.string.language), languages.firstOrNull { it.first == model.language }?.second ?: languages.first().second, onClick = { dialog = "language" })
                        SettingsRow(getString(R.string.settings_mode), getString(if (KV.get(KV_APPLICATION_MODE) == AppMode.Classic) R.string.classic_version else R.string.new_version), onClick = { dialog = "mode" })
                        if (UserManager.session() != null) SettingsRow(getString(R.string.import_legacy), onClick = { startActivity(Intent(this@SettingActivity, ImportLegacyActivity::class.java)) })
                    }
                    SettingsSection(getString(R.string.settings_developer)) {
                        SettingsRow(getString(R.string.account_server), onClick = { if (model.developer) dialog = "account_host" else developerLocked() })
                        SettingsRow(getString(R.string.daymark_server), onClick = { if (model.developer) dialog = "server_host" else developerLocked() })
                        if (model.developer) SettingsRow(getString(R.string.disable_developer), onClick = { model.disableDeveloper() })
                    }
                    SettingsSection(getString(R.string.settings_other)) {
                        SettingsRow(getString(R.string.settings_version), com.creepersan.memorialday.BuildConfig.VERSION_NAME, showArrow = false)
                        SettingsRow(getString(R.string.about_app), onClick = { startActivity(Intent(this@SettingActivity, AboutActivity::class.java)) })
                        SettingsRow(getString(R.string.agreement_privacy), onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LEGAL_DOCUMENT_URL))) })
                        SettingsRow(getString(R.string.agreement_terms), onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LEGAL_DOCUMENT_URL))) })
                    }
                    Text(getString(R.string.settings_copyright), color = glassSecondaryColor(), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                }
                when (dialog) {
                    "theme" -> GlassSelectionDialog(getString(R.string.theme), themes, model.theme.name, { value -> dialog = null; model.changeTheme(AppTheme.valueOf(value)) }, { dialog = null })
                    "language" -> GlassSelectionDialog(getString(R.string.language), languages, model.language, { value -> dialog = null; model.changeLanguage(value); recreate() }, { dialog = null })
                    "mode" -> GlassSelectionDialog(getString(R.string.settings_mode), listOf(AppMode.Default.name to getString(R.string.new_version), AppMode.Classic.name to getString(R.string.classic_version)), KV.get(KV_APPLICATION_MODE).name, { value ->
                        dialog = null
                        val mode = AppMode.valueOf(value)
                        if (mode != KV.get(KV_APPLICATION_MODE)) {
                            KV.set(KV_APPLICATION_MODE, mode)
                            startActivity(Intent(this@SettingActivity, com.creepersan.memorialday.activity.dispatch.DispatchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            finish()
                        }
                    }, { dialog = null })
                    "account_host", "server_host" -> {
                        val account = dialog == "account_host"
                        var value by remember(dialog) { mutableStateOf(if (account) model.accountHost else model.serverHost) }
                        androidx.compose.ui.window.Dialog(onDismissRequest = { dialog = null; hostMessage = null }) {
                            GlassCard(solid = true) {
                                Text(getString(if (account) R.string.account_server else R.string.daymark_server), fontSize = 17.sp)
                                Text(getString(R.string.proxy_description), fontSize = 12.sp, color = glassSecondaryColor(), modifier = Modifier.padding(vertical = 12.dp))
                                GlassTextField(value, { value = it }, getString(if (account) R.string.account_server else R.string.daymark_server))
                                hostMessage?.let { Text(getString(it), fontSize = 12.sp) }
                                GlassButton(getString(R.string.save), {
                                    val old = if (account) model.accountHost else model.serverHost
                                    if (account) model.accountHost = value else model.serverHost = value
                                    if (model.saveHosts()) { dialog = null; hostMessage = null } else {
                                        if (account) model.accountHost = old else model.serverHost = old
                                        hostMessage = R.string.invalid_url
                                    }
                                })
                            }
                        }
                    }
                }
            } }
        }
    }
}

/** 设置分组使用设计稿中的半透明卡片。 */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, color = glassSecondaryColor(), modifier = Modifier.padding(bottom = 8.dp))
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White.copy(alpha = if (isGlassDark()) 0.08f else 0.35f), shape).border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = if (isGlassDark()) 0.14f else 0.65f), shape).padding(6.dp), content = content)
    }
}

/** 设置项保留整行点击区域，状态值和箭头使用次要色。 */
@Composable
private fun SettingsRow(label: String, value: String = "", danger: Boolean = false, centered: Boolean = false, showArrow: Boolean = true, onClick: (() -> Unit)? = null) {
    val secondary = glassSecondaryColor()
    Row(Modifier.fillMaxWidth().height(56.dp).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (centered) Spacer(Modifier.weight(1f))
        Text(label, fontSize = 14.sp, color = if (danger) androidx.compose.ui.graphics.Color(0xFFE07866) else MaterialTheme.colorScheme.onBackground)
        if (!centered) Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, fontSize = 13.sp, color = secondary, maxLines = 1)
        if (showArrow && !centered) Text("›", fontSize = 21.sp, color = secondary, modifier = Modifier.padding(start = 8.dp))
        if (centered) Spacer(Modifier.weight(1f))
    }
}
