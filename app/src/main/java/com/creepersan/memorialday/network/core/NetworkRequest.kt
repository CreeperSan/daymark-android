package com.creepersan.memorialday.network.core

import com.creepersan.memorialday.network.middleware.NetworkMiddleware
import com.creepersan.memorialday.network.transfer.RequestPayload
import com.creepersan.memorialday.network.transfer.ResponseDecoder
import java.time.Duration

/**
 * 单次网络请求的不可变描述。
 *
 * 请求级配置仅作用于本对象，不会修改 [NetworkClient] 的默认配置。
 */
data class NetworkRequest<T>(
    val method: HttpMethod,
    val path: String,
    val responseDecoder: ResponseDecoder<T>,
    val queryParameters: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap(),
    val payload: RequestPayload = RequestPayload.Empty,
    val timeout: Duration? = null,
    val middlewares: List<NetworkMiddleware> = emptyList()
) {
    init {
        require(path.isNotBlank()) { "请求路径不能为空" }
        require(path.startsWith("/").not()) {
            "请求路径不能以 / 开头，请使用相对路径以避免覆盖基础地址路径"
        }
        require(timeout == null || !timeout.isZero && !timeout.isNegative) {
            "请求超时必须为正数"
        }
    }
}

/** HTTP 请求方法。 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD
}
