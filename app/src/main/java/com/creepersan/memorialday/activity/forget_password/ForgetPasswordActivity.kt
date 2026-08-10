package com.creepersan.memorialday.activity.forget_password

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.auth.CSAuthPage
import com.creepersan.memorialday.module.route.navigator.CSNavigator
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSInput
import com.creepersan.memorialday.widget.composable.CSTextHint

class ForgetPasswordActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorialDayTheme {
                var email by rememberSaveable {
                    mutableStateOf(CSNavigator.forgotPasswordEmail(intent))
                }
                CSAuthPage(
                    title = "找回密码",
                    subtitle = "通过邮箱重置你的登录密码",
                    showBack = true,
                    onBack = { CSNavigator.close(this) }
                ) {
                    CSInput(
                        value = email,
                        onValueChange = { email = it },
                        label = "邮箱",
                        placeholder = "name@example.com"
                    )
                    CSTextHint("密码找回服务暂未开放，请联系支持人员。")
                    CSButton(
                        text = "服务暂未开放",
                        fullWidth = true,
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}