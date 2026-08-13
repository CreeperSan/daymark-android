package com.creepersan.memorialday.common.activity

import androidx.appcompat.app.AppCompatActivity
import com.creepersan.memorialday.application.DaymarkApplication

open class BaseActivity: AppCompatActivity() {

    private val daymarkApplication get() = (application as DaymarkApplication)

    private val sessionManager get() = daymarkApplication.userStore

    private val applicationScope get() = daymarkApplication.applicationScope.value

}