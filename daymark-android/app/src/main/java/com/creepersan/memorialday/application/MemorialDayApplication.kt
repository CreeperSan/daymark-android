package com.creepersan.memorialday.application

import android.app.Application
import com.creepersan.memorialday.database.DatabaseManager
import com.creepersan.memorialday.module.network.NetworkManager
import com.creepersan.memorialday.module.network.CommonHeaderInterceptor
import com.creepersan.memorialday.module.network.AuthenticationInterceptor
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.module.log.Log
import android.app.Activity
import android.os.Bundle

class MemorialDayApplication : Application() {

    companion object {
        lateinit var instance: MemorialDayApplication
    }

    lateinit var databaseManager: DatabaseManager

    override fun onCreate() {
        super.onCreate()

        instance = this
        Log.info("应用启动", "Application")
        // 统一记录页面生命周期，覆盖经典版和新版页面。
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) { Log.info("页面创建：${activity.javaClass.simpleName}", "Lifecycle") }
            override fun onActivityStarted(activity: Activity) { }
            override fun onActivityResumed(activity: Activity) { com.creepersan.memorialday.ui.toast.ToastManager.onActivityResumed(activity); Log.info("页面显示：${activity.javaClass.simpleName}", "Lifecycle") }
            override fun onActivityPaused(activity: Activity) { com.creepersan.memorialday.ui.toast.ToastManager.onActivityPaused(activity); Log.info("页面暂停：${activity.javaClass.simpleName}", "Lifecycle") }
            override fun onActivityStopped(activity: Activity) { }
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) { }
            override fun onActivityDestroyed(activity: Activity) { Log.info("页面销毁：${activity.javaClass.simpleName}", "Lifecycle") }
        })

        initDatabase()
        // 公共设备信息与认证信息分别维护，刷新请求也保留设备信息。
        NetworkManager.addInterceptor(CommonHeaderInterceptor(this))
        NetworkManager.addInterceptor(AuthenticationInterceptor { UserManager.validSession().map { it.token }.blockingGet() })
    }

    private fun initDatabase() {
        databaseManager = DatabaseManager()
    }

}


