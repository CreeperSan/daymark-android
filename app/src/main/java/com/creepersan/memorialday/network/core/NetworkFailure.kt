package com.creepersan.memorialday.network.core

/**
 * 统一描述网络框架可预期的失败类型。
 *
 * [requestId] 仅用于关联脱敏诊断信息，绝不包含认证凭据或请求体内容。
 */
sealed class NetworkFailure(
    open val message: String,
    open val cause: Throwable? = null,
    open val requestId: String? = null
) {
    data class Configuration(
        override val message: String,
        override val cause: Throwable? = null,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Connection(
        override val message: String,
        override val cause: Throwable,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Timeout(
        override val message: String,
        override val cause: Throwable,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Http(
        val code: Int,
        val responseBody: String?,
        override val message: String,
        override val cause: Throwable? = null,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Decode(
        override val message: String,
        override val cause: Throwable,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class File(
        override val message: String,
        override val cause: Throwable,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Middleware(
        override val message: String,
        override val cause: Throwable? = null,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Sse(
        override val message: String,
        override val cause: Throwable? = null,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)

    data class Cancelled(
        override val message: String = "请求已取消",
        override val cause: Throwable? = null,
        override val requestId: String? = null
    ) : NetworkFailure(message, cause, requestId)
}
