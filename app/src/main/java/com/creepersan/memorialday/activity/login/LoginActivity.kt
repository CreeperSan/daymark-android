package com.creepersan.memorialday.activity.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.auth.CSAuthDivider
import com.creepersan.memorialday.activity.auth.CSAuthPage
import com.creepersan.memorialday.activity.auth.CSAuthUnavailableSocialLogin
import com.creepersan.memorialday.activity.business.CSBusinessActivity
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

class LoginActivity : BaseComposeActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initializeAccount(CSNavigator.loginAccount(intent))
        setContent {
            MemorialDayTheme {
                LoginPage(
                    viewModel = viewModel,
                    onLoginSucceeded = ::onLoginSucceeded
                )
            }
        }
    }

    private fun onLoginSucceeded(account: String) {
        startActivity(Intent(this, CSBusinessActivity::class.java))
        CSNavigator.close(
            this,
            CSPageExitModel(
                outcome = CSPageOutcome.Completed,
                data = Bundle().apply { putString("cs_login_account", account) }
            )
        )
    }

@Composable
private fun LoginPage(
    viewModel: LoginViewModel,
    onLoginSucceeded: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Succeeded -> onLoginSucceeded(event.account)
            }
        }
    }

    CSAuthPage(
        title = "欢迎回来",
        subtitle = "继续记录你的每一个重要日子",
        footer = {
            CSButtonText(
                text = "还没有账号？注册新账号",
                onClick = {
                    scope.launch {
                        CSNavigator.toRegister(context as LoginActivity).collect {}
                    }
                }
            )
        }
    ) {
        CSInput(
            value = uiState.account,
            onValueChange = viewModel::onAccountChanged,
            label = "邮箱或账号",
            placeholder = "name@example.com",
            supportingText = uiState.error,
            isError = uiState.error != null
        )
        CSPasswordInput(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = "密码",
            placeholder = "请输入密码",
            supportingText = null,
            isError = false
        )
        if (uiState.loading) {
            CSButtonLoading(text = "登录中", fullWidth = true)
        } else {
            CSButton(
                text = "登 录",
                fullWidth = true,
                onClick = viewModel::login
            )
        }
    }
}
}