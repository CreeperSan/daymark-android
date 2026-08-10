package com.creepersan.memorialday.activity.intro

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import com.creepersan.memorialday.common.activity.BaseComposeActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.business.CSBusinessActivity
import com.creepersan.memorialday.module.network.host.account.KVAccountTokenStorage

class IntroActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Scaffold() { insets ->

            }
        }

        val destination = if (KVAccountTokenStorage().current() == null) {
            LoginActivity::class.java
        } else {
            CSBusinessActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}