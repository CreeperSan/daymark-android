package com.creepersan.memorialday.activity.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.creepersan.memorialday.activity.login.LoginActivity
import org.junit.Rule
import org.junit.Test

class AuthenticationActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<LoginActivity>()

    @Test
    fun login_screen_exposes_labeled_inputs_and_validation_error() {
        composeRule.onNodeWithText("欢迎回来").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("邮箱或账号").performTextInput("alice")
        composeRule.onNodeWithText("登 录").performClick()
        composeRule.onNodeWithText("密码不能为空").assertIsDisplayed()
    }
}
