package com.creepersan.memorialday.module.http.request

import com.creepersan.memorialday.module.http.codec.ResponseDecoder
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import java.time.Duration

/**
 * HTTP 请求方法。
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS
}

/**
 * 请求体描述。
 *
 * 核心层只描述数据，不暴露 OkHttp 的 RequestBody，具体编码由传输实现完成。
 */
sealed interface RequestBody {
    /**
     * JSON 请求体。
     */
    data class Json(val value: Any) : RequestBody

    /**
     * 表单请求体。
     */
    data class Form(val fields: Map<String, String>) : RequestBody

    /**
     * Multipart 请求体。
     */
    data class Multipart(val parts: List<Part>) : RequestBody {
        /**
         * Multipart 的单个字段或文件部分。
         */
        data class Part(
            val name: String,
            val value: String? = null,
            val filePath: String? = null,
            val fileName: String? = null,
            val contentType: String? = null
        )
    }

    /**
     * 二进制请求体。
     */
    data class Binary(
        val bytes: ByteArray,
        val contentType: String = "application/octet-stream"
    ) : RequestBody

    /**
     * 文件请求体。
     */
    data class File(
        val path: String,
        val contentType: String = "application/octet-stream"
    ) : RequestBody
}

/**
 * 进度采样配置。
 */
data class ProgressConfig(
    val minIntervalMillis: Long = 100L,
    val minBytesDelta: Long = 64 * 1024L
) {
    init {
        require(minIntervalMillis >= 0) { "进度最小时间间隔不能为负数" }
        require(minBytesDelta >= 0) { "进度最小字节增量不能为负数" }
    }
}

/**
 * 不可变的网络请求描述。
 *
 * 构造时会复制所有集合，避免调用方在请求执行期间修改请求配置。
 */
data class NetworkRequest<T>(
    val host: String? = null,
    val path: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val query: Map<String, String?> = emptyMap(),
    val body: RequestBody? = null,
    val attributes: RequestAttributes = RequestAttributes.empty(),
    val middlewares: List<NetworkMiddleware> = emptyList(),
    val decoder: ResponseDecoder<T>? = null,
    val timeout: Duration? = null,
    val progressConfig: ProgressConfig = ProgressConfig()
) {
    init {
        require(path.isNotBlank()) { "请求路径不能为空" }
        require(timeout == null || !timeout.isNegative && !timeout.isZero) {
            "请求超时必须为正数"
        }
    }

    /**
     * 复制请求配置，确保外部集合的后续修改不会影响本请求。
     */
    fun snapshot(): NetworkRequest<T> = copy(
        headers = headers.toMap(),
        query = query.toMap(),
        attributes = attributes.snapshot(),
        middlewares = middlewares.toList()
    )

}
