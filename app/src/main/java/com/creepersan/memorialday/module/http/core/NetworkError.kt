package com.creepersan.memorialday.module.http.core

import java.io.IOException

/**
 * 网络框架统一错误模型。
 *
 * 错误消息只包含安全诊断信息，不包含认证头、Cookie、密码和完整请求体。
 */
sealed class NetworkError(
    open val requestId: String? = null,
    open val cause: Throwable? = null
) {
    /**
     * 请求配置错误。
     */
    data class Configuration(
        val reason: String,
        override val requestId: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(requestId, cause)

    /**
     * 网络连接错误。
     */
    data class Connection(
        val reason: String,
        override val requestId: String? = null,
        override val cause: IOException? = null
    ) : NetworkError(requestId, cause)

    /**
     * 网络超时错误。
     */
    data class Timeout(
        val reason: String,
        override val requestId: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(requestId, cause)

    /**
     * HTTP 状态错误。
     */
    data class Http(
        val statusCode: Int,
        val bodySummary: String? = null,
        override val requestId: String? = null
    ) : NetworkError(requestId)

    /**
     * 响应解码错误。
     */
    data class Decode(
        val reason: String,
        override val requestId: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(requestId, cause)

    /**
     * 业务协议错误。
     */
    data class Business(
        val code: String,
        val reason: String,
        override val requestId: String? = null
    ) : NetworkError(requestId)

    /**
     * 文件传输错误。
     */
    data class File(
        val reason: String,
        override val requestId: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(requestId, cause)

    /**
     * SSE 协议或连接错误。
     */
    data class Sse(
        val reason: String,
        val retryable: Boolean = false,
        override val requestId: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(requestId, cause)

    /**
     * 协程取消，不应被转成普通失败。
     */
    data class Cancelled(
        override val requestId: String? = null
    ) : NetworkError(requestId)
}
