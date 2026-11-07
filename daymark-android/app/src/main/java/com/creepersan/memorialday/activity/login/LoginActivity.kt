package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
                Text(getString(R.string.login_title), fontSize = 32.sp)
                Text(getString(R.string.login_description))
                GlassCard {
                    OutlinedTextField(value = model.account, onValueChange = { model.account = it }, label = { Text(getString(R.string.account)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = model.password, onValueChange = { model.password = it }, label = { Text(getString(R.string.password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    GlassButton(getString(if (model.loading) R.string.logging_in else R.string.login), { model.login() }, !model.loading && model.account.isNotBlank() && model.password.isNotBlank())
                }
                Row { TextButton(onClick = {}, enabled = false) { Text(getString(R.string.register)) }; TextButton(onClick = {}, enabled = false) { Text(getString(R.string.forgot_password)) } }
                Text(getString(R.string.coming_soon), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { startActivity(Intent(this@LoginActivity, SettingActivity::class.java)) }) { Text(getString(R.string.settings)) }
            }
        }
    }
}
