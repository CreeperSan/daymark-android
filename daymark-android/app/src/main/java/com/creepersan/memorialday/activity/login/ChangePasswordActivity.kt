package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.user.PasswordEndpointUnavailableException
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.module.user.UserManager.MIN_PASSWORD_LENGTH
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

/** 保留修改密码的输入和提交状态，旋转屏幕时继续等待同一次请求。 */
class ChangePasswordViewModel : ViewModel() {
    var oldPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmation by mutableStateOf("")
    var loading by mutableStateOf(false); private set
    var done by mutableStateOf(false); private set
    var error by mutableStateOf<Throwable?>(null); private set
    private var request: Disposable? = null

    val canSubmit get() = !loading && !done && oldPassword.isNotEmpty() && newPassword.length >= MIN_PASSWORD_LENGTH && confirmation == newPassword

    /** 发送原密码和已确认的新密码，失败时保留表单以便重试。 */
    fun submit() {
        if (!canSubmit) return
        loading = true; error = null
        request = UserManager.changePassword(oldPassword, newPassword).observeOn(AndroidSchedulers.mainThread()).subscribe({
            oldPassword = ""; newPassword = ""; confirmation = ""; loading = false; done = true
        }, { loading = false; error = it })
    }

    override fun onCleared() { request?.dispose() }
}

/** 已登录用户的修改密码入口，服务端吊销会话后返回登录页。 */
class ChangePasswordActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[ChangePasswordViewModel::class.java]
        if (UserManager.session() == null && !model.loading && !model.done) { finish(); return }
        setContent {
            if (model.done) LaunchedEffect(Unit) {
                if (UserManager.session() == null) {
                    Toast.makeText(this@ChangePasswordActivity, R.string.account_password_changed, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ChangePasswordActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }
                finish()
            }
            // 提交过程中保留页面，确保收到会话失效的结果后再返回登录。
            BackHandler(enabled = model.loading) { }
            GlassPage {
                GlassNavigationBar(getString(R.string.settings_change_password), { if (!model.loading) finish() })
                Column(Modifier.weight(1f).glassBounce().verticalScroll(rememberScrollState())) {
                    GlassCard {
                        Text(getString(R.string.account_password_change_hint), color = glassSecondaryColor())
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(model.oldPassword, { model.oldPassword = it }, getString(R.string.account_old_password), password = true)
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(model.newPassword, { model.newPassword = it }, getString(R.string.account_new_password), password = true)
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(model.confirmation, { model.confirmation = it }, getString(R.string.account_confirm_password), password = true)
                        if (model.confirmation.isNotEmpty() && model.confirmation != model.newPassword) {
                            Text(getString(R.string.account_password_mismatch), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                        }
                        model.error?.let { error ->
                            val message = if (error is PasswordEndpointUnavailableException) getString(R.string.account_password_unavailable) else error.message ?: getString(R.string.connection_error)
                            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        GlassButton(getString(if (model.loading) R.string.account_wait else R.string.confirm), { model.submit() }, model.canSubmit)
                    }
                }
            }
        }
    }
}
