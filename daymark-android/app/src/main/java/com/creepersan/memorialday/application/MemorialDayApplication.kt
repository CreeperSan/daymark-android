package com.creepersan.memorialday.application

import android.app.Application
import com.creepersan.memorialday.database.DatabaseManager
import com.creepersan.memorialday.module.network.NetworkManager
import com.creepersan.memorialday.module.network.CommonHeaderInterceptor
import com.creepersan.memorialday.module.network.AuthenticationInterceptor
import com.creepersan.memorialday.module.user.UserManager

class MemorialDayApplication : Application() {

    companion object {
        lateinit var instance: MemorialDayApplication
    }

    lateinit var databaseManager: DatabaseManager

    override fun onCreate() {
        super.onCreate()

        instance = this

        initDatabase()
        // 公共设备信息与认证信息分别维护，刷新请求也保留设备信息。
        NetworkManager.addInterceptor(CommonHeaderInterceptor(this))
        NetworkManager.addInterceptor(AuthenticationInterceptor { UserManager.validSession().map { it.token }.blockingGet() })
    }

    private fun initDatabase() {
        databaseManager = DatabaseManager()
    }

}


