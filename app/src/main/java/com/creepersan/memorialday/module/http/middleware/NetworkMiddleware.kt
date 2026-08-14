package com.creepersan.memorialday.module.http.middleware

import com.creepersan.memorialday.module.http.core.NetworkError
import com.creepersan.memorialday.module.http.request.NetworkRequest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call

/**
 * 单次请求执行上下文。
 *
 * 每次收集都会创建新上下文，因此并发请求之间不会共享重试次数、SSE 标识或属性。
 */
class RequestContext internal constructor(
    val requestId: String = UUID.randomUUID().toString(),
    val attributes: MutableRequestAttributes = MutableRequestAttributes()
) {
    /** 请求开始时间。 */
    val startedAtMillis: Long = System.currentTimeMillis()

    /** 已执行的重连次数。 */
    var reconnectAttempts: Int = 0

    /** 最近一次成功交付的 SSE 事件标识。 */
    var lastEventId: String? = null

    /**
     * 传输层进度回调，由上层 Flow 执行器在每次收集时设置。
     */
    var progressSink: ((ProgressDirection, Long, Long?) -> Unit)? = null

    private val activeCall = AtomicReference<Call?>()

    /**
     * 绑定底层调用，供取消流程主动中断网络请求。
     */
    internal fun attachCall(call: Call) {
        activeCall.set(call)
    }

    /**
     * 清理已完成的底层调用。
     */
    internal fun detachCall(call: Call) {
        activeCall.compareAndSet(call, null)
    }

    /**
     * 取消当前底层调用。
     */
    internal fun cancelCall() {
        activeCall.getAndSet(null)?.cancel()
    }
}

/**
 * 进度方向。
 */
enum class ProgressDirection {
    /** 上传方向。 */
    UPLOAD,

    /** 下载方向。 */
    DOWNLOAD
}

/**
 * 中间件执行期间可读写的类型化上下文属性。
 */
class MutableRequestAttributes {
    private val values = ConcurrentHashMap<String, Any?>()

    /**
     * 写入属性。
     */
    fun <T : Any> put(key: ContextKey<T>, value: T) {
        values[key.name] = value
    }

    /**
     * 读取属性。
     */
    fun <T : Any> get(key: ContextKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values[key.name] as? T
    }
}

/**
 * 中间件上下文属性键。
 */
class ContextKey<T : Any>(val name: String)

/**
 * 中间件控制结果。
 */
sealed interface MiddlewareResult {
    /**
     * 继续执行下游。
     */
    data class Proceed<T>(val request: NetworkRequest<T>) : MiddlewareResult

    /**
     * 使用中间件生成的结果短路。
     */
    data class Success(val value: Any?) : MiddlewareResult

    /**
     * 以结构化错误终止。
     */
    data class Failure(val error: NetworkError) : MiddlewareResult

    /**
     * 取消本次执行。
     */
    data object Cancelled : MiddlewareResult
}

/**
 * 可挂起的应用层网络中间件。
 */
fun interface NetworkMiddleware {
    /**
     * 处理请求，可在调用 [chain.proceed] 前后挂起或修改请求。
     */
    suspend fun intercept(
        request: NetworkRequest<*>,
        context: RequestContext,
        chain: NetworkMiddlewareChain
    ): MiddlewareResult
}

/**
 * 中间件链继续入口。
 */
fun interface NetworkMiddlewareChain {
    /**
     * 继续执行下一个中间件或传输层。
     */
    suspend fun proceed(
        request: NetworkRequest<*>,
        context: RequestContext
    ): MiddlewareResult
}

/**
 * 线程安全的全局中间件注册表。
 */
class MiddlewareRegistry {
    private val middlewares = AtomicReference<List<NetworkMiddleware>>(emptyList())

    /**
     * 按注册顺序追加全局中间件。
     */
    fun register(middleware: NetworkMiddleware) {
        middlewares.updateAndGet { current -> current + middleware }
    }

    /**
     * 移除指定实例。
     */
    fun unregister(middleware: NetworkMiddleware) {
        middlewares.updateAndGet { current -> current - middleware }
    }

    /**
     * 返回本次请求使用的不可变快照。
     */
    fun snapshot(): List<NetworkMiddleware> = middlewares.get().toList()
}
