package com.creepersan.memorialday.base.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

open class BaseComposeActivity: ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() // 开启页面沉浸
        super.onCreate(savedInstanceState)
    }

}