package com.creepersan.memorialday.activity.intro

import android.content.Intent
import android.os.Bundle
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import com.creepersan.memorialday.module.network.host.account.KVAccountTokenStorage

class IntroActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = if (KVAccountTokenStorage().current() == null) {
            LoginActivity::class.java
        } else {
            CSBusinessActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}