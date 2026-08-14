package com.creepersan.memorialday.module.http.stream

import com.creepersan.memorialday.module.http.core.NetworkError
import kotlinx.coroutines.flow.Flow

/**
 * 原始字节流事件。
 */
sealed interface ByteStreamEvent {
    /**
     * 流已建立。
     */
    data class Started(
        val requestId: String,
        val statusCode: Int,
        val headers: Map<String, String>
    ) : ByteStreamEvent

    /**
     * 一个有序字节块。
     */
    data class Chunk(
        val bytes: ByteArray,
        val offset: Long
    ) : ByteStreamEvent

    /**
     * 传输进度。
     */
    data class Progress(
        val transferred: Long,
        val total: Long?
    ) : ByteStreamEvent

    /**
     * 正常结束。
     */
    data object Completed : ByteStreamEvent

    /**
     * 流执行失败。
     */
    data class Failed(val error: NetworkError) : ByteStreamEvent

    /**
     * 流被取消。
     */
    data object Cancelled : ByteStreamEvent
}

/**
 * 原始字节流调用。
 */
interface ByteStreamCall {
    /**
     * 冷事件流，只有收集时才建立连接。
     */
    val events: Flow<ByteStreamEvent>
}

/**
 * 文件下载结果。
 */
data class DownloadResult(
    val targetPath: String,
    val bytes: Long,
    val resumed: Boolean
)
