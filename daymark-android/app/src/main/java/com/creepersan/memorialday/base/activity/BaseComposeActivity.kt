package com.creepersan.memorialday.base.activity

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import java.util.Locale

/** 新版页面使用独立语言配置，避免影响经典版。 */
open class BaseComposeActivity : ComponentActivity() {
    private var appliedLanguage: String? = null
    private var appliedTheme: String? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // 新版页面自行处理安全区域，允许背景和滚动内容延伸到系统栏后方。
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val preferences = getSharedPreferences("application", Context.MODE_PRIVATE)
        appliedLanguage = preferences.getString("language", "system")
        appliedTheme = preferences.getString("theme", "system")
    }

    override fun onResume() {
        super.onResume()
        val preferences = getSharedPreferences("application", Context.MODE_PRIVATE)
        if (appliedLanguage != preferences.getString("language", "system") || appliedTheme != preferences.getString("theme", "system")) recreate()
    }

    override fun attachBaseContext(newBase: Context) {
        // Application 尚未初始化时也可直接读取语言配置。
        val language = newBase.getSharedPreferences("application", Context.MODE_PRIVATE).getString("language", "system") ?: "system"
        if (language == "system") { super.attachBaseContext(newBase); return }
        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
}
