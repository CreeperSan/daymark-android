package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.home.HomeActivity
import com.creepersan.memorialday.activity.setting.SettingActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.constance.KV_REMEMBERED_ACCOUNT
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

/** 登录表单状态及请求。 */
class LoginViewModel : ViewModel() {
    var account by mutableStateOf(KV.get(KV_REMEMBERED_ACCOUNT)); var rememberAccount by mutableStateOf(account.isNotEmpty()); var password by mutableStateOf("")
    var loading by mutableStateOf(false); var error by mutableStateOf<String?>(null); var success by mutableStateOf(false)
    private var request: Disposable? = null
    fun login() {
        if (loading || account.isBlank() || password.isBlank()) return
        loading = true; error = null
        request = UserManager.login(account.trim(), password).observeOn(AndroidSchedulers.mainThread()).subscribe({ loading = false; KV.set(KV_REMEMBERED_ACCOUNT, if (rememberAccount) account.trim() else ""); password = ""; success = true }, { loading = false; error = it.message ?: "" })
    }
    override fun onCleared() { request?.dispose() }
}

@OptIn(ExperimentalMaterial3Api::class)
/** 新版账号登录页面。 */
class LoginActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[LoginViewModel::class.java]
        setContent {
            if (model.success) LaunchedEffect(Unit) { com.creepersan.memorialday.ui.toast.ToastManager.message(this@LoginActivity, getString(R.string.login_success)); startActivity(Intent(this@LoginActivity, HomeActivity::class.java)); finish() }
            LaunchedEffect(model.error) { model.error?.let { com.creepersan.memorialday.ui.toast.ToastManager.message(this@LoginActivity, it.ifBlank { getString(R.string.login_failed) }); model.error = null } }
            if (model.loading) androidx.compose.ui.window.Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
                GlassCard(solid = true) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Text(getString(R.string.logging_in)) } }
            }
            GlassPage {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().heightIn(min = maxHeight).glassBounce().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(60.dp).background(Color.White.copy(alpha = if (isGlassDark()) 0.12f else 0.55f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Text("♥", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(getString(R.string.login_title), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text(getString(R.string.login_description), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(Modifier.height(28.dp))
                GlassCard {
                    GlassTextField(model.account, { model.account = it }, getString(R.string.account))
                    Spacer(Modifier.height(12.dp))
                    GlassTextField(model.password, { model.password = it }, getString(R.string.password), password = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text((if (model.rememberAccount) "☑  " else "□  ") + getString(R.string.login_remember), fontSize = 13.sp, color = glassSecondaryColor(), modifier = Modifier.glassClickable { model.rememberAccount = !model.rememberAccount }.padding(vertical = 12.dp))
                        Text(getString(R.string.forgot_password), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.glassClickable { startActivity(Intent(this@LoginActivity, AccountActivity::class.java)) }.padding(vertical = 12.dp))
                    }
                    GlassButton(getString(R.string.login), { model.login() }, !model.loading && model.account.isNotBlank() && model.password.isNotBlank())
                }
                Spacer(Modifier.height(26.dp))
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(getString(R.string.login_no_account), fontSize = 13.sp, color = glassSecondaryColor())
                    Text(getString(R.string.register), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.glassClickable { startActivity(Intent(this@LoginActivity, AccountActivity::class.java).putExtra(AccountActivity.EXTRA_REGISTER, true)) }.padding(8.dp))
                    Text(getString(R.string.settings), fontSize = 13.sp, color = glassSecondaryColor(), modifier = Modifier.glassClickable { startActivity(Intent(this@LoginActivity, SettingActivity::class.java)) }.padding(8.dp))
                }
                Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
