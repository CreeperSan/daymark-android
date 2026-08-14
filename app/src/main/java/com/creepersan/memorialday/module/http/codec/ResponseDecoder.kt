package com.creepersan.memorialday.module.http.codec

import com.creepersan.memorialday.module.http.transport.RawNetworkResponse

/**
 * 将传输层原始响应转换为业务模型。
 *
 * 解码器不依赖具体 HTTP 客户端，业务协议 envelope 也应在这里完成校验。
 */
fun interface ResponseDecoder<T> {
    /**
     * 解码一次性响应。
     */
    suspend fun decode(response: RawNetworkResponse): T
}

/**
 * 常用文本响应解码器。
 */
object TextResponseDecoder : ResponseDecoder<String> {
    override suspend fun decode(response: RawNetworkResponse): String = response.bodyText()
}
