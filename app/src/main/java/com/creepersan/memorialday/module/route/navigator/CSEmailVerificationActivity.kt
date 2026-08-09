package com.creepersan.memorialday.module.route.navigator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.creepersan.memorialday.activity.auth.CSAuthPage
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.network.host.account.AccountRepositoryFactory
import com.creepersan.memorialday.module.network.host.account.AccountResult
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSButtonLoading
import com.creepersan.memorialday.widget.composable.CSButtonText
import com.creepersan.memorialday.widget.composable.CSOtpInput
import com.creepersan.memorialday.widget.composable.CSTextHint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CSEmailVerificationActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorialDayTheme {
                EmailVerificationPage(
                    email = CSNavigator.verificationEmail(intent),
                    initialExpiry = CSNavigator.verificationExpiresAt(intent),
                    onClose = { CSNavigator.close(this) },
                    onConfirmed = { code ->
                        CSNavigator.close(this, CSNavigator.verificationCodeResult(code))
                    }
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmailVerificationPage(
    email: String,
    initialExpiry: Long,
    onClose: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AccountRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var resendLoading by rememberSaveable { mutableStateOf(false) }
    var expiresAt by rememberSaveable { mutableLongStateOf(initialExpiry) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(expiresAt) {
        while (expiresAt > System.currentTimeMillis()) {
            remainingSeconds = ((expiresAt - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L)
            delay(1_000L)
        }
        remainingSeconds = 0L
    }

    CSAuthPage(
        title = "验证你的邮箱",
        subtitle = "验证码已发送至 $email",
        showBack = true,
        onBack = onClose
    ) {
        CSOtpInput(
            value = code,
            onValueChange = { code = it; error = null },
            isError = error != null
        )
        error?.let { CSTextHint(it, color = com.creepersan.memorialday.widget.composable.token.CSTheme.colors.danger) }
        CSButton(
            text = "确认验证码",
            fullWidth = true,
            enabled = code.length == 6,
            onClick = { onConfirmed(code) }
        )
        if (resendLoading) {
            CSButtonLoading(text = "重新发送中", fullWidth = true)
        } else {
            CSButtonText(
                text = if (remainingSeconds > 0L) "重新发送（${remainingSeconds}s）" else "重新发送验证码",
                enabled = remainingSeconds == 0L,
                onClick = {
                    resendLoading = true
                    scope.launch {
                        when (val result = repository.requestAuthCode(email)) {
                            is AccountResult.Success -> expiresAt = result.data.expireTime ?: 0L
                            is AccountResult.Failure -> error = result.error.message
                        }
                        resendLoading = false
                    }
                }
            )
        }
    }
}
