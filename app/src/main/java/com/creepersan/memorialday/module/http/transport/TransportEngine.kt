package com.creepersan.memorialday.module.http.transport

import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import kotlinx.coroutines.flow.Flow

/**
 * 传输层原始响应。
 */
interface RawNetworkResponse : AutoCloseable {
    /** HTTP 状态码。 */
    val statusCode: Int

    /** 响应头。 */
    val headers: Map<String, String>

    /** 响应媒体类型。 */
    val contentType: String?

    /**
     * 将响应体读取为文本。
     */
    suspend fun bodyText(): String

    /**
     * 将响应体读取为字节数组，仅适合小响应。
     */
    suspend fun bodyBytes(): ByteArray

    override fun close()
}

/**
 * 流式响应资源。
 */
interface NetworkStream : AutoCloseable {
    /** HTTP 状态码。 */
    val statusCode: Int

    /** 响应头。 */
    val headers: Map<String, String>

    /** 响应媒体类型。 */
    val contentType: String?

    /** 响应总长度，未知时为空。 */
    val contentLength: Long?

    /**
     * 以有序字节块读取响应。
     */
    fun chunks(bufferSize: Int = DEFAULT_BUFFER_SIZE): Flow<ByteArray>

    override fun close()

    companion object {
        /** 默认读取缓冲区大小。 */
        const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024
    }
}

/**
 * 可替换的网络传输引擎。
 */
interface TransportEngine {
    /**
     * 执行一次性请求。
     */
    suspend fun execute(
        request: NetworkRequest<*>,
        requestBody: RequestBody?,
        context: com.creepersan.memorialday.module.http.middleware.RequestContext
    ): RawNetworkResponse

    /**
     * 打开流式响应。
     */
    suspend fun openStream(
        request: NetworkRequest<*>,
        requestBody: RequestBody?,
        context: com.creepersan.memorialday.module.http.middleware.RequestContext
    ): NetworkStream
}
