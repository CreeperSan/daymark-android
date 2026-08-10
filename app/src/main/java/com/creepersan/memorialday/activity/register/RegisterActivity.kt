package com.creepersan.memorialday.activity.register

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.auth.CSAuthDivider
import com.creepersan.memorialday.activity.auth.CSAuthPage
import com.creepersan.memorialday.activity.auth.CSAuthUnavailableSocialLogin
import com.creepersan.memorialday.module.network.host.account.AccountRepositoryFactory
import com.creepersan.memorialday.module.network.host.account.AccountResult
import com.creepersan.memorialday.module.route.navigator.CSEmailVerificationPageEnter
import com.creepersan.memorialday.module.route.navigator.CSNavigator
import com.creepersan.memorialday.module.route.navigator.CSPageExitModel
import com.creepersan.memorialday.module.route.navigator.CSPageOutcome
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSButtonLoading
import com.creepersan.memorialday.widget.composable.CSButtonText
import com.creepersan.memorialday.widget.composable.CSCheckbox
import com.creepersan.memorialday.widget.composable.CSInput
import com.creepersan.memorialday.widget.composable.CSPasswordInput
import kotlinx.coroutines.launch

class RegisterActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorialDayTheme {
                RegisterPage(
                    initialAccount = CSNavigator.registerAccount(intent),
                    initialEmail = CSNavigator.registerEmail(intent),
                    onClose = { CSNavigator.close(this) }
                )
            }
        }
    }

@androidx.compose.runtime.Composable
private fun RegisterPage(
    initialAccount: String,
    initialEmail: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AccountRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var account by rememberSaveable { mutableStateOf(initialAccount) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }
    CSAuthPage(
        title = "创建账号",
        subtitle = "开始记录每一个值得铭记的日子",
        showBack = true,
        onBack = onClose,
        footer = {
            CSButtonText(
                text = "已有账号？直接登录",
                onClick = { CSNavigator.toLogin(context as RegisterActivity).subscribe() }
            )
        }
    ) {
        CSInput(
            value = account,
            onValueChange = { account = it; error = null },
            label = "昵称",
            placeholder = "给自己起个名字",
            supportingText = error,
            isError = error != null
        )
        CSInput(
            value = email,
            onValueChange = { email = it; error = null },
            label = "邮箱",
            placeholder = "name@example.com"
        )
        CSPasswordInput(
            value = password,
            onValueChange = { password = it; error = null },
            label = "密码",
            placeholder = "至少 8 位，含字母和数字"
        )
        CSCheckbox(
            checked = acceptedTerms,
            onCheckedChange = { acceptedTerms = it },
            label = "我已阅读并同意《用户协议》和《隐私政策》"
        )
        if (loading) {
            CSButtonLoading(text = "发送验证码中", fullWidth = true)
        } else {
            CSButton(
                text = "注 册",
                fullWidth = true,
                onClick = {
                    if (!acceptedTerms) {
                        error = "请先阅读并同意用户协议和隐私政策"
                        return@CSButton
                    }
                    loading = true
                    scope.launch {
                        when (val availability = repository.checkUserExists(account, email)) {
                            is AccountResult.Failure -> {
                                error = availability.error.message
                                loading = false
                            }
                            is AccountResult.Success -> {
                                if (availability.data.accountExists == true || availability.data.emailExists == true) {
                                    error = "账号或邮箱已被注册"
                                    loading = false
                                    return@launch
                                }
                                when (val codeResult = repository.requestAuthCode(email)) {
                                    is AccountResult.Failure -> {
                                        error = codeResult.error.message
                                        loading = false
                                    }
                                    is AccountResult.Success -> {
                                        loading = false
                                        CSNavigator.toEmailVerification(
                                            context as RegisterActivity,
                                            CSEmailVerificationPageEnter(
                                                account = account,
                                                email = email,
                                                expiresAtMillis = codeResult.data.expireTime ?: 0L
                                            )
                                        ).subscribe { exit ->
                                            if (exit.outcome == CSPageOutcome.Completed && exit.code != null) {
                                                scope.launch {
                                                    loading = true
                                                    when (val registration = repository.register(account, password, email, exit.code)) {
                                                        is AccountResult.Success -> CSNavigator.close(
                                                            context,
                                                            CSPageExitModel(CSPageOutcome.Completed)
                                                        )
                                                        is AccountResult.Failure -> {
                                                            error = registration.error.message
                                                            loading = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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