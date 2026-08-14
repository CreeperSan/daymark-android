package com.creepersan.memorialday.module.http.core

/**
 * 网络日志接口。
 *
 * 实现方必须自行保证输出目标安全；框架只提供脱敏后的请求标识、方法和路径信息。
 */
fun interface NetworkLogger {
    /**
     * 输出一条网络诊断日志。
     */
    fun log(message: String)

    companion object {
        /**
         * 默认空日志器，避免基础库直接依赖 Android 日志组件。
         */
        val Noop: NetworkLogger = NetworkLogger { }
    }
}
