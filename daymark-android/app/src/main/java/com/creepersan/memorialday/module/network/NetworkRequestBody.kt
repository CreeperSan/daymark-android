package com.creepersan.memorialday.module.network

/** 支持的请求体类型。JSON 可传入 Map、列表或普通数据对象。 */
sealed class NetworkRequestBody {
    class Json(val value: Any) : NetworkRequestBody()
    class Form(val values: Map<String, String>) : NetworkRequestBody()
    class Text(val value: String, val contentType: String = "text/plain; charset=utf-8") : NetworkRequestBody()
    class Binary(val value: ByteArray, val contentType: String = "application/octet-stream") : NetworkRequestBody()
}
