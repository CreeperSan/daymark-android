package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.auth.CSAuthDivider
import com.creepersan.memorialday.activity.auth.CSAuthPage
import com.creepersan.memorialday.activity.auth.CSAuthUnavailableSocialLogin
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import com.creepersan.memorialday.module.network.host.account.AccountRepositoryFactory
import com.creepersan.memorialday.module.network.host.account.AccountResult
import com.creepersan.memorialday.module.route.navigator.CSNavigator
import com.creepersan.memorialday.module.route.navigator.CSPageExitModel
import com.creepersan.memorialday.module.route.navigator.CSPageOutcome
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSButtonLoading
import com.creepersan.memorialday.widget.composable.CSButtonText
import com.creepersan.memorialday.widget.composable.CSInput
import com.creepersan.memorialday.widget.composable.CSPasswordInput
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class LoginActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorialDayTheme {
                LoginPage(
                    initialAccount = CSNavigator.loginAccount(intent),
                    onLoginSucceeded = { account ->
                        startActivity(Intent(this, CSBusinessActivity::class.java))
                        CSNavigator.close(
                            this,
                            CSPageExitModel(
                                outcome = CSPageOutcome.Completed,
                                data = Bundle().apply { putString("cs_login_account", account) }
                            )
                        )
                    },
                    onClose = { CSNavigator.close(this) }
                )
            }
        }
    }

@androidx.compose.runtime.Composable
private fun LoginPage(
    initialAccount: String,
    onLoginSucceeded: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AccountRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var account by rememberSaveable { mutableStateOf(initialAccount) }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }
    CSAuthPage(
        title = "欢迎回来",
        subtitle = "继续记录你的每一个重要日子",
        footer = {
            CSButtonText(
                text = "还没有账号？注册新账号",
                onClick = {
                    CSNavigator.toRegister(context as LoginActivity).subscribe()
                }
            )
        }
    ) {
        CSInput(
            value = account,
            onValueChange = { account = it; error = null },
            label = "邮箱或账号",
            placeholder = "name@example.com",
            supportingText = error,
            isError = error != null
        )
        CSPasswordInput(
            value = password,
            onValueChange = { password = it; error = null },
            label = "密码",
            placeholder = "请输入密码",
            supportingText = null,
            isError = false
        )
        CSButtonText(
            text = "忘记密码？",
            onClick = { CSNavigator.toForgotPassword(context as LoginActivity).subscribe() }
        )
        if (loading) {
            CSButtonLoading(text = "登录中", fullWidth = true)
        } else {
            CSButton(
                text = "登 录",
                fullWidth = true,
                onClick = {
                    loading = true
                    scope.launch {
                        when (val result = repository.login(account, password)) {
                            is AccountResult.Success -> onLoginSucceeded(result.data.account ?: account)
                            is AccountResult.Failure -> {
                                error = result.error.message
                                loading = false
                            }
                        }
                    }
                }
            )
        }
        CSAuthDivider()
        CSAuthUnavailableSocialLogin()
    }
}
}