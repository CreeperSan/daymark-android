package com.creepersan.memorialday.module.network.host.account

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Identifies the app installation to the account service. It is deliberately injectable so
 * tests and non-Android hosts never need to depend on Android framework state.
 */
interface AccountDeviceInfoProvider {
    fun appName(): String
    fun platform(): String
    fun deviceCode(): String
    fun deviceName(): String
    fun versionCode(): String
}

class AndroidAccountDeviceInfoProvider(
    private val context: Context
) : AccountDeviceInfoProvider {

    override fun appName(): String = APP_NAME

    override fun platform(): String = PLATFORM_ANDROID

    override fun deviceCode(): String {
        return requireNotNull(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ) { "Android ID is unavailable" }.also {
            require(it.isNotBlank()) { "Android ID is unavailable" }
        }
    }

    override fun deviceName(): String = Build.MODEL.ifBlank { Build.DEVICE }

    override fun versionCode(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.longVersionCode.toString()
    }

    private companion object {
        const val APP_NAME = "Daymark"
        const val PLATFORM_ANDROID = "Android"
    }
}
