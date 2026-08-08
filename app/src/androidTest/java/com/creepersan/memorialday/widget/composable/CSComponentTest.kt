package com.creepersan.memorialday.widget.composable

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.Text
import androidx.test.core.app.ActivityScenario
import com.creepersan.memorialday.widget.composable.token.CSTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CSComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun progress_exposes_accessibility_description() {
        composeRule.setContent {
            CSTheme {
                CSProgressBar(progress = 2f, contentDescription = "上传进度")
            }
        }

        composeRule.onNodeWithContentDescription("上传进度").assertIsDisplayed()
    }

    @Test
    fun disabled_button_does_not_invoke_action() {
        var clicked = false
        composeRule.setContent {
            CSTheme {
                CSButton("禁用", enabled = false, onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("禁用").assertIsNotEnabled()
        assertFalse(clicked)
    }

    @Test
    fun empty_state_renders_title() {
        composeRule.setContent {
            CSTheme {
                CSEmptyState(title = "暂无纪念日")
            }
        }

        composeRule.onNodeWithText("暂无纪念日").assertIsDisplayed()
    }

    @Test
    fun empty_list_renders_custom_empty_content() {
        composeRule.setContent {
            CSTheme {
                CSList(
                    items = emptyList<String>(),
                    emptyContent = { Text("没有记录") },
                    itemContent = { Text(it) }
                )
            }
        }

        composeRule.onNodeWithText("没有记录").assertIsDisplayed()
    }

    @Test
    fun switch_exposes_role_and_callback() {
        var checked = false
        composeRule.setContent {
            CSTheme {
                CSSwitch(
                    checked = false,
                    onCheckedChange = { checked = it },
                    contentDescription = "启用提醒"
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("启用提醒")
            .assertIsDisplayed()
            .performClick()
        assertTrue(checked)
    }

    @Test
    fun reduced_motion_exposes_static_configuration() {
        composeRule.setContent {
            CSTheme(reducedMotion = true) {
                Text(if (CSTheme.motion.enabled) "animated" else "static")
            }
        }

        composeRule.onNodeWithText("static").assertIsDisplayed()
    }

    @Test
    fun showcase_activity_launches_without_business_dependencies() {
        ActivityScenario.launch(CSComponentShowcaseActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
        }
    }
}
