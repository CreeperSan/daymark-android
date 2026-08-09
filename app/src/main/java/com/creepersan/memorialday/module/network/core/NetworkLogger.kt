package com.creepersan.memorialday.module.network.core

import android.util.Log

/**
 * 可注入的网络诊断输出接口。
 *
 * 框架只提供已脱敏的字符串，调用方不得在实现中重新记录原始认证信息或载荷。
 */
fun interface NetworkLogger {
    fun log(message: String)

    companion object {
        /** 默认不输出任何内容，避免生产环境意外泄露信息。 */
        val NONE = NetworkLogger { }

        /** 输出到 Android Logcat 的网络诊断日志。 */
        val LOGCAT = NetworkLogger { message -> Log.d("NetworkClient", message) }
    }
}
