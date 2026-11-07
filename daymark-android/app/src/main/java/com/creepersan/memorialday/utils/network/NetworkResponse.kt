package com.creepersan.memorialday.utils.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.nio.charset.StandardCharsets

/** HTTP 响应；非 2xx 状态码也会返回响应，供调用方读取错误信息。 */
class NetworkResponse(
    val responseCode: Int,
    val responseMessage: String,
    val responseBody: ByteArray,
    val responseHeaders: Map<String, List<String>>,
) {
    /** 按响应头指定的字符集读取文本，未指定时使用 UTF-8。 */
    fun bodyString(): String {
        val contentType = responseHeaders.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value?.firstOrNull()
        val charset = contentType?.toMediaTypeOrNull()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        return responseBody.toString(charset)
    }
}
