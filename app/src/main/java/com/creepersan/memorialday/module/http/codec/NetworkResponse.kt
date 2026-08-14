package com.creepersan.memorialday.module.http.codec

/**
 * 未配置响应解码器时返回的原始响应快照。
 *
 * 快照中的 body 已脱离底层响应资源，调用方无需负责关闭 OkHttp 响应体。
 */
data class NetworkResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val contentType: String?,
    val body: ByteArray
) {
    /**
     * 以 UTF-8 文本读取响应内容。
     */
    fun bodyText(): String = body.toString(Charsets.UTF_8)
}
