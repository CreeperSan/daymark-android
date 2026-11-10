package com.creepersan.memorialday.module.network

import io.reactivex.rxjava3.core.Observable
import com.creepersan.memorialday.BuildConfig
import com.creepersan.memorialday.module.log.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/** 统一执行网络请求，拦截器使用 OkHttp 的应用拦截器。 */
object NetworkManager {

    private const val DEFAULT_TIMEOUT_SECONDS = 30L
    private const val LOG_TAG = "DaymarkNetwork"
    private const val LOG_CHUNK_SIZE = 3000
    private const val JSON_CONTENT_TYPE = "application/json; charset=utf-8"

    private val gson = Gson()
    private val client = OkHttpClient()
    // 登录和刷新请求不能与等待鉴权的请求共享调度队列，否则可能发生饥饿。
    private val authenticationClient = client.newBuilder().dispatcher(Dispatcher()).build()
    private val interceptors = CopyOnWriteArrayList<Interceptor>()

    /** 异步发送请求；取消订阅会取消对应的 OkHttp 请求。网络结果在后台线程回调，参数错误在订阅线程回调。 */
    fun request(request: NetworkRequest): Observable<NetworkResponse> {
        return Observable.create { emitter ->
            try {
                val url = request.host.toHttpUrl().resolve(request.url)
                    ?: throw IllegalArgumentException("无效的请求地址：${request.url}")
                val urlBuilder = url.newBuilder()
                request.query?.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value.toString()) }

                val body = createBody(request.data)
                require(request.method != NetworkRequestMethod.Get || body == null) { "GET 请求不支持请求体" }
                val requestBuilder = Request.Builder().url(urlBuilder.build()).tag(NetworkAuthentication::class.java, request.authentication)
                request.headers?.forEach { (key, value) -> requestBuilder.header(key, value) }
                val needsBody = request.method == NetworkRequestMethod.Post || request.method == NetworkRequestMethod.Put || request.method == NetworkRequestMethod.Patch
                requestBuilder.method(request.method.value, body ?: if (needsBody) ByteArray(0).toRequestBody() else null)

                val timeout = request.timeout?.toLong() ?: DEFAULT_TIMEOUT_SECONDS
                require(timeout > 0) { "请求超时时间必须大于零" }
                val clientBuilder = (if (request.authentication is NetworkAuthentication.Required) client else authenticationClient).newBuilder()
                    .callTimeout(timeout, TimeUnit.SECONDS)
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                interceptors.forEach { clientBuilder.addInterceptor(it) }
                val prepared = requestBuilder.build()
                val address = NetworkLogFormatter.url(prepared.url)
                Log.info("请求开始：${request.method.value} ${prepared.url.encodedPath}", LOG_TAG)
                if (BuildConfig.DEBUG) debugLog("请求 ${request.method.value} $address\n路由 Host：${prepared.header("Host") ?: prepared.url.host}\n参数：${NetworkLogFormatter.body(request.data)}")
                val call = clientBuilder.build().newCall(prepared)
                emitter.setCancellable { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.warning("请求失败：${prepared.url.encodedPath}，${e.javaClass.simpleName}", LOG_TAG)
                        if (BuildConfig.DEBUG) debugLog("请求失败 $address：${e.javaClass.simpleName}")
                        // 取消后的错误无需再通知已经取消订阅的调用方。
                        emitter.tryOnError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use {
                                val result = NetworkResponse(it.code, it.message, it.body?.bytes() ?: ByteArray(0), it.headers.toMultimap())
                                Log.info("请求完成：${prepared.url.encodedPath}，HTTP ${result.responseCode}", LOG_TAG)
                                if (BuildConfig.DEBUG) debugLog("响应 $address：HTTP ${result.responseCode}\n结果：${NetworkLogFormatter.json(result.bodyString(), preserveStatusCode = true)}")
                                if (!emitter.isDisposed) {
                                    emitter.onNext(result)
                                    emitter.onComplete()
                                }
                            }
                        } catch (e: IOException) {
                            Log.warning("读取响应失败：${prepared.url.encodedPath}，${e.javaClass.simpleName}", LOG_TAG)
                            emitter.tryOnError(e)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.error("请求准备失败：${e.javaClass.simpleName}", LOG_TAG)
                emitter.tryOnError(e)
            }
        }
    }

    /** 长响应分段输出，避免被 Logcat 的单条长度限制截断；仅供 Debug 调用。 */
    private fun debugLog(message: String) {
        try { message.chunked(LOG_CHUNK_SIZE).forEach { android.util.Log.d(LOG_TAG, it) } }
        catch (_: RuntimeException) { /* 本地 JVM 测试没有 Android Log 实现。 */ }
    }

    /** 添加公共拦截器；变更仅影响之后创建的请求。 */
    fun addInterceptor(interceptor: Interceptor) {
        interceptors.addIfAbsent(interceptor)
    }

    /** 移除公共拦截器。 */
    fun removeInterceptor(interceptor: Interceptor) {
        interceptors.remove(interceptor)
    }

    /** 将请求体转换为对应的 HTTP 内容。 */
    private fun createBody(data: NetworkRequestBody?): RequestBody? {
        return when (data) {
            null -> null
            is NetworkRequestBody.Json -> gson.toJson(data.value).toRequestBody(JSON_CONTENT_TYPE.toMediaType())
            is NetworkRequestBody.Form -> FormBody.Builder().apply {
                data.values.forEach { (key, value) -> add(key, value) }
            }.build()
            is NetworkRequestBody.Text -> data.value.toRequestBody(data.contentType.toMediaType())
            is NetworkRequestBody.Binary -> data.value.toRequestBody(data.contentType.toMediaType())
        }
    }
}
