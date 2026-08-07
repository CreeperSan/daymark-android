package com.creepersan.memorialday.network.core

import com.creepersan.memorialday.network.middleware.NetworkMiddleware
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.Duration

/**
 * 网络客户端的不可变配置。
 */
class NetworkClientConfig internal constructor(
    val baseUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val writeTimeout: Duration,
    val defaultHeaders: Map<String, String>,
    val okHttpClient: OkHttpClient,
    val gson: Gson,
    val globalMiddlewares: List<NetworkMiddleware>,
    val logger: NetworkLogger
)

/**
 * 构建 [NetworkClient] 的入口。
 */
class NetworkClientBuilder {
    private var baseUrl: String? = null
    private var connectTimeout = DEFAULT_TIMEOUT
    private var readTimeout = DEFAULT_TIMEOUT
    private var writeTimeout = DEFAULT_TIMEOUT
    private var defaultHeaders: Map<String, String> = emptyMap()
    private var okHttpClient: OkHttpClient = OkHttpClient()
    private var gson: Gson = Gson()
    private var globalMiddlewares: List<NetworkMiddleware> = emptyList()
    private var logger: NetworkLogger = NetworkLogger.NONE

    /** 设置客户端基础地址，必须为 http 或 https URL。 */
    fun baseUrl(value: String) = apply {
        baseUrl = value
    }

    /** 设置连接超时。 */
    fun connectTimeout(value: Duration) = apply {
        connectTimeout = value
    }

    /** 设置读取超时。 */
    fun readTimeout(value: Duration) = apply {
        readTimeout = value
    }

    /** 设置写入超时。 */
    fun writeTimeout(value: Duration) = apply {
        writeTimeout = value
    }

    /** 设置每个请求默认携带的请求头。 */
    fun defaultHeaders(value: Map<String, String>) = apply {
        defaultHeaders = value.toMap()
    }

    /** 设置传输层 OkHttp 客户端。 */
    fun okHttpClient(value: OkHttpClient) = apply {
        okHttpClient = value
    }

    /** 设置 JSON 编解码器。 */
    fun gson(value: Gson) = apply {
        gson = value
    }

    /** 设置全局中间件，执行顺序即列表顺序。 */
    fun globalMiddlewares(value: List<NetworkMiddleware>) = apply {
        globalMiddlewares = value.toList()
    }

    /** 设置脱敏后的网络诊断输出实现。 */
    fun logger(value: NetworkLogger) = apply {
        logger = value
    }

    /** 验证配置并创建独立客户端。 */
    fun build(): NetworkClient {
        val resolvedBaseUrl = requireNotNull(baseUrl) { "必须配置基础地址" }
        require(resolvedBaseUrl.toHttpUrlOrNull() != null) { "基础地址格式非法" }
        requirePositive(connectTimeout, "连接超时")
        requirePositive(readTimeout, "读取超时")
        requirePositive(writeTimeout, "写入超时")
        return NetworkClient(
            NetworkClientConfig(
                baseUrl = resolvedBaseUrl.ensureTrailingSlash(),
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                defaultHeaders = defaultHeaders,
                okHttpClient = okHttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout)
                    .build(),
                gson = gson,
                globalMiddlewares = globalMiddlewares,
                logger = logger
            )
        )
    }

    private fun requirePositive(value: Duration, name: String) {
        require(!value.isZero && !value.isNegative) { "${name}必须为正数" }
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    private companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
