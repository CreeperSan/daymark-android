package com.creepersan.memorialday.module.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.creepersan.memorialday.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/** 为所有请求添加服务端识别应用和设备所需的公共请求头。 */
class CommonHeaderInterceptor(private val context: Context) : Interceptor {
    companion object {
        private const val APP_NAME = "Daymark"
        private const val PLATFORM = "Android"
        private const val UNKNOWN_DEVICE = "unknown"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceCode = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UNKNOWN_DEVICE
        val request = chain.request().newBuilder()
            .header("CS-App", APP_NAME)
            .header("CS-Device-Code", deviceCode)
            .header("CS-Device-Name", Build.MODEL)
            .header("CS-Device-Platform", PLATFORM)
            .header("CS-Version", BuildConfig.VERSION_CODE.toString())
            .build()
        return chain.proceed(request)
    }
}
