package com.creepersan.memorialday.module.network.middleware

import com.creepersan.memorialday.module.network.core.NetworkFailure
import java.util.concurrent.ConcurrentHashMap

/**
 * 支持协程挂起的应用层网络中间件。
 *
 * 鉴权刷新、业务模型转换和请求级日志必须使用此接口；同步的 OkHttp 拦截器只负责传输层工作。
 */
fun interface NetworkMiddleware {
    suspend fun intercept(context: RequestContext, chain: NetworkMiddlewareChain): MiddlewareOutcome
}

/**
 * 单个请求独享的可变上下文。
 *
 * 不包含请求体和认证原文，避免中间件之间或并发请求之间意外泄露敏感信息。
 */
class RequestContext internal constructor(
    val requestId: String,
    val headers: MutableMap<String, String>,
    val tags: Map<String, String>
) {
    private val attributes = ConcurrentHashMap<String, Any>()

    /** 写入仅限当前请求可见的扩展数据。 */
    fun putAttribute(key: String, value: Any) {
        attributes[key] = value
    }

    /** 读取当前请求的扩展数据。 */
    fun getAttribute(key: String): Any? = attributes[key]
}

/** 中间件控制后续请求生命周期的结果。 */
sealed interface MiddlewareOutcome {
    /** 继续执行链中的下一个中间件或底层传输。 */
    data object Continue : MiddlewareOutcome

    /** 使用中间件产生的结果短路底层网络传输。 */
    data class ShortCircuit(val data: Any?) : MiddlewareOutcome

    /** 阻止请求并将给定失败作为终态。 */
    data class Failure(val error: NetworkFailure) : MiddlewareOutcome

    /** 终止请求且不再执行后续处理。 */
    data object Cancelled : MiddlewareOutcome
}

/**
 * 中间件链的继续入口。
 *
 * 调用 [proceed] 可以挂起等待，并在恢复后返回下游的终态决定。
 */
fun interface NetworkMiddlewareChain {
    suspend fun proceed(context: RequestContext): MiddlewareOutcome
}
