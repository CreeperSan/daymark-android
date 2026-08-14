package com.creepersan.memorialday.module.http.sse

import com.creepersan.memorialday.module.http.core.NetworkError
import kotlinx.coroutines.flow.Flow

/**
 * 原始 SSE 事件帧。
 */
data class RawSseEvent(
    val event: String?,
    val id: String?,
    val retryMillis: Long?,
    val data: String
)

/**
 * SSE 类型解码器。
 */
fun interface SseEventDecoder<T> {
    /**
     * 将 SSE data 字段转换为业务模型。
     */
    suspend fun decode(event: RawSseEvent): T
}

/**
 * SSE 重连策略。
 */
sealed interface ReconnectPolicy {
    /**
     * 不重连。
     */
    data object Disabled : ReconnectPolicy

    /**
     * 限定次数的指数退避重连。
     */
    data class Limited(
        val maxAttempts: Int,
        val initialDelayMillis: Long = 1_000L,
        val maxDelayMillis: Long = 30_000L
    ) : ReconnectPolicy {
        init {
            require(maxAttempts >= 0) { "最大重连次数不能为负数" }
            require(initialDelayMillis >= 0) { "初始重连等待不能为负数" }
            require(maxDelayMillis >= initialDelayMillis) {
                "最大重连等待不能小于初始等待"
            }
        }
    }
}

/**
 * SSE 调用配置。
 */
data class SseOptions(
    val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.Disabled,
    val initialLastEventId: String? = null
)

/**
 * SSE 事件流事件。
 */
sealed interface SseEvent<out T> {
    /**
     * 连接建立。
     */
    data class Started(
        val requestId: String,
        val statusCode: Int,
        val headers: Map<String, String>
    ) : SseEvent<Nothing>

    /**
     * 已解析的业务消息。
     */
    data class Message<T>(
        val event: String?,
        val id: String?,
        val retryMillis: Long?,
        val data: T
    ) : SseEvent<T>

    /**
     * 收到协议心跳，不携带业务数据。
     */
    data object Heartbeat : SseEvent<Nothing>

    /**
     * 连接正常结束。
     */
    data object Completed : SseEvent<Nothing>

    /**
     * 连接失败。
     */
    data class Failed(val error: NetworkError) : SseEvent<Nothing>

    /**
     * 调用方取消连接。
     */
    data object Cancelled : SseEvent<Nothing>
}

/**
 * SSE 调用接口。
 */
interface SseCall<T> {
    /**
     * 冷 SSE 事件流。
     */
    val events: Flow<SseEvent<T>>
}
