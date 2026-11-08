package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

/** 登录表单状态及请求。 */
class LoginViewModel : ViewModel() {
    var account by mutableStateOf(""); var password by mutableStateOf("")
    var loading by mutableStateOf(false); var error by mutableStateOf<String?>(null); var success by mutableStateOf(false)
    private var request: Disposable? = null
    fun login() {
        if (loading || account.isBlank() || password.isBlank()) return
        loading = true; error = null
        request = UserManager.login(account.trim(), password).observeOn(AndroidSchedulers.mainThread()).subscribe({ loading = false; password = ""; success = true }, { loading = false; error = it.message })
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
            if (model.success) LaunchedEffect(Unit) { startActivity(Intent(this@LoginActivity, HomeActivity::class.java)); finish() }
            GlassPage {
                Spacer(Modifier.height(28.dp))
                Box(Modifier.size(60.dp).background(Color(0xFFF28C70), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text("♥", color = Color.White, fontSize = 28.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(getString(R.string.login_title), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(getString(R.string.login_description), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(Modifier.height(28.dp))
                GlassCard {
                    GlassTextField(model.account, { model.account = it }, getString(R.string.account))
                    Spacer(Modifier.height(12.dp))
                    GlassTextField(model.password, { model.password = it }, getString(R.string.password), password = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(getString(R.string.forgot_password), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { startActivity(Intent(this@LoginActivity, AccountActivity::class.java)) }.padding(vertical = 12.dp))
                    }
                    model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    GlassButton(getString(if (model.loading) R.string.logging_in else R.string.login), { model.login() }, !model.loading && model.account.isNotBlank() && model.password.isNotBlank())
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(getString(R.string.register), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { startActivity(Intent(this@LoginActivity, AccountActivity::class.java).putExtra(AccountActivity.EXTRA_REGISTER, true)) }.padding(12.dp))
                    Text(getString(R.string.settings), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { startActivity(Intent(this@LoginActivity, SettingActivity::class.java)) }.padding(12.dp))
                }
            }
        }
    }
}
