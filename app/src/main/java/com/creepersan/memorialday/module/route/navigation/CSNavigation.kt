package com.creepersan.memorialday.module.route.navigation

import android.app.Activity
import android.content.Intent
import com.creepersan.memorialday.activity.login.LoginActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

object CSNavigation {


    fun replaceToLoginActivity(activity: Activity) {
        activity.startActivity(Intent(activity, LoginActivity::class.java))
        activity.finish()
    }


}