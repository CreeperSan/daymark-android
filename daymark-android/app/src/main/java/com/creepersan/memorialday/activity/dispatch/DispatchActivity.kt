package com.creepersan.memorialday.activity.dispatch

import android.content.Intent
import android.os.Bundle
import com.creepersan.memorialday.activity.home.HomeActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.main.MainActivity
import com.creepersan.memorialday.activity.onboarding.OnboardingActivity
import com.creepersan.memorialday.activity.select_mode.SelectModeActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.*
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.module.user.UserManager

/** 启动时根据已保存模式和会话分流。 */
class DispatchActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatch()
    }

    /** 按本地模式与会话分流，首页加载时由请求拦截器校验会话。 */
    private fun dispatch() {
        // 旧版本只记录运行模式，将其迁移为旧欢迎页版本，避免老用户重复看到相同内容。
        if (!KV.has(KV_WELCOME_SCREEN_SHOW_VERSION) && KV.has(KV_APPLICATION_MODE)) {
            KV.set(KV_WELCOME_SCREEN_SHOW_VERSION, WELCOME_SCREEN_LEGACY_VERSION)
        }
        when {
            KV.get(KV_WELCOME_SCREEN_SHOW_VERSION) < WELCOME_SCREEN_CURRENT_VERSION -> open(OnboardingActivity::class.java)
            !KV.has(KV_APPLICATION_MODE) -> open(SelectModeActivity::class.java)
            KV.get(KV_APPLICATION_MODE) == AppMode.Classic -> open(MainActivity::class.java)
            UserManager.session() == null -> open(LoginActivity::class.java)
            else -> open(HomeActivity::class.java)
        }
    }
    private fun open(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }
}
