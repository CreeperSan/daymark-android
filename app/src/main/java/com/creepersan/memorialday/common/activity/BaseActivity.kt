package com.creepersan.memorialday.common.activity

import androidx.appcompat.app.AppCompatActivity
import com.creepersan.memorialday.application.DaymarkApplication

open class BaseActivity: AppCompatActivity() {

    val daymarkApplication get() = (application as DaymarkApplication)

    val userStore get() = daymarkApplication.userStore

    val appStateStore get() = daymarkApplication.appStateStore

    val deviceInfoStore get() = daymarkApplication.deviceInfoStore

    val applicationScope get() = daymarkApplication.applicationScope.value

}