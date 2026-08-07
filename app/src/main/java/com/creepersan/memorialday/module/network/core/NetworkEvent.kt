package com.creepersan.memorialday.module.network.core

/**
 * 网络执行期间向 Flow 发送的状态事件。
 *
 * 每次执行只会产生一个终态事件：成功、失败或取消。
 */
sealed interface NetworkEvent<out T> {

    /** 请求已通过配置校验并开始执行。 */
    data object Started : NetworkEvent<Nothing>

    /** 上传或下载的累计传输进度。 */
    data class Progress(
        val direction: Direction,
        val transferredBytes: Long,
        val totalBytes: Long?
    ) : NetworkEvent<Nothing> {
        enum class Direction {
            UPLOAD,
            DOWNLOAD
        }

        val percentage: Int?
            get() = totalBytes
                ?.takeIf { it > 0L }
                ?.let { ((transferredBytes * 100L) / it).toInt().coerceIn(0, 100) }
    }

    /** 服务端发送事件流中的单个消息。 */
    data class SseMessage(
        val event: String?,
        val id: String?,
        val data: String,
        val retryMillis: Long?
    ) : NetworkEvent<Nothing>

    /** 请求成功完成并返回已解析的数据。 */
    data class Success<T>(val data: T) : NetworkEvent<T>

    /** 请求以可诊断的失败结束。 */
    data class Failure(val error: NetworkFailure) : NetworkEvent<Nothing>

    /** 请求被收集协程、调用方或中间件取消。 */
    data object Cancelled : NetworkEvent<Nothing>
}
