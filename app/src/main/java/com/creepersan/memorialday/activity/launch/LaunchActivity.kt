package com.creepersan.memorialday.activity.launch

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleCoroutineScope
import com.creepersan.memorialday.activity.intro.IntroActivity
import com.creepersan.memorialday.activity.main.MainActivity
import com.creepersan.memorialday.common.activity.BaseActivity
import com.creepersan.memorialday.common.definitions.AppMode
import com.creepersan.memorialday.common.storage.kv.KVStorageApplication_AppMode
import com.creepersan.memorialday.module.route.navigator.CSNavigator
import com.creepersan.memorialday.module.storage.kv.get
import com.creepersan.memorialday.module.storage.kv.set

/**
 * 统一路由处理页面
 *
 * 需要兼容的情况如下：
 *
 * 1. 应用正常启动
 * 2. 经典版本用户升级引导
 * 3. url打开应用特定页面（未实现，规划中）
 * 4. 广告显示（未实现，规划中）
 * 5. 过期版本拦截（未实现，规划中）
 */
@SuppressLint("CustomSplashScreen")
class LaunchActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)

		// 信息收集
		val appMode = KVStorageApplication_AppMode.get()


		// 应用正常启动 & 引导用户升级
		when (appMode) {
			/* 经典版 */
			AppMode.Classic -> {
				startActivity(Intent(this, MainActivity::class.java))
			}
			/* 服务器版 */
			AppMode.Server -> {
				startActivity(Intent(this, IntroActivity::class.java))
			}
			/* 未确定 */
			AppMode.NotDecide -> {
				val hasOldData = true
				if (hasOldData) {
					startActivity(Intent(this, IntroActivity::class.java))
				} else {
					startActivity(Intent(this, IntroActivity::class.java))
				}
			}
		}

		finish()
	}


}