package com.creepersan.memorialday.activity.launch

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.creepersan.memorialday.activity.intro.IntroActivity
import com.creepersan.memorialday.common.activity.BaseActivity

class LaunchActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)

		startActivity(Intent(this, IntroActivity::class.java))
		finish()
	}
}