package com.creepersan.memorialday.network.core

import com.creepersan.memorialday.network.internal.MiddlewareChainExecutor
import com.creepersan.memorialday.network.middleware.MiddlewareOutcome
import com.creepersan.memorialday.network.middleware.RequestContext
import com.creepersan.memorialday.network.transfer.DefaultPayloadCodec
import com.creepersan.memorialday.network.transfer.ProgressReporter
import com.creepersan.memorialday.network.transfer.ProgressRequestBody
import com.creepersan.memorialday.network.transfer.ResponseDecoders
import com.creepersan.memorialday.network.transfer.SseOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.File
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/**
 * 面向业务层的网络客户端。
 *
 * 客户端本身不可变且线程安全；所有单请求覆盖信息都保存在 [NetworkRequest] 中。
 */
class NetworkClient internal constructor(
    private val config: NetworkClientConfig
) {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(config.okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(config.gson))
        .build()

    private val payloadCodec = DefaultPayloadCodec(config.gson)

    /** 根据 Retrofit 声明接口创建服务实例。 */
    fun <T : Any> createService(service: Class<T>): T = retrofit.create(service)

    /**
     * 执行普通请求并以冷 Flow 报告状态、进度与终态。
     */
    fun <T> execute(request: NetworkRequest<T>): Flow<NetworkEvent<T>> = channelFlow {
        val activeCall = AtomicReference<Call?>()
        try {
            trySend(NetworkEvent.Started)
            val context = RequestContext(
                requestId = UUID.randomUUID().toString(),
                headers = config.defaultHeaders.toMutableMap().apply { putAll(request.headers) },
                tags = request.tags
            )
            config.logger.log(
                "网络请求开始 requestId=${context.requestId} method=${request.method} path=${request.path}"
            )
            val outcome = MiddlewareChainExecutor(
                middlewares = config.globalMiddlewares + request.middlewares,
                terminal = {
                    executeTransport(
                        request = request,
                        context = it,
                        activeCall = activeCall,
                        eventScope = this
                    )
                }
            ).proceed(context)
            emitOutcome(outcome)
            config.logger.log("网络请求结束 requestId=${context.requestId} outcome=${outcome::class.simpleName}")
        } catch (cancelled: CancellationException) {
            activeCall.getAndSet(null)?.cancel()
            trySend(NetworkEvent.Cancelled)
            throw cancelled
        } catch (failure: Throwable) {
            trySend(NetworkEvent.Failure(toFailure(failure, null)))
        } finally {
            activeCall.getAndSet(null)?.cancel()
        }
    }

    /**
     * 下载文件到目标位置。
     *
     * 启用 [resume] 时会以临时文件长度请求范围下载；服务端未返回 206 时会安全地重新下载。
     */
    fun download(
        path: String,
        target: File,
        resume: Boolean = true,
        headers: Map<String, String> = emptyMap()
    ): Flow<NetworkEvent<File>> {
        val temporary = File(target.parentFile, "${target.name}.download")
        val resumeHeaders = headers.toMutableMap().apply {
            if (resume && temporary.length() > 0L) {
                put("Range", "bytes=${temporary.length()}-")
            }
        }
        return execute(
            NetworkRequest(
                method = HttpMethod.GET,
                path = path,
                responseDecoder = ResponseDecoders.file(target, resume = resume),
                headers = resumeHeaders
            )
        )
    }

    /**
     * 建立 Server-Sent Events 长连接。
     *
     * 事件源与收集协程绑定，取消收集会立即关闭连接。重连次数达到上限后返回 SSE 失败终态。
     */
    fun sse(
        request: NetworkRequest<*>,
        options: SseOptions = SseOptions()
    ): Flow<NetworkEvent<Nothing>> = channelFlow {
        val context = RequestContext(
            requestId = UUID.randomUUID().toString(),
            headers = config.defaultHeaders.toMutableMap().apply {
                putAll(request.headers)
                put("Accept", "text/event-stream")
                options.initialLastEventId?.let { put("Last-Event-ID", it) }
            },
            tags = request.tags
        )
        val activeSource = AtomicReference<EventSource?>()
        val reconnectAttempts = AtomicInteger(0)
        var lastEventId = options.initialLastEventId

        fun connect() {
            val sourceRequest = buildRequest(
                request = request,
                context = context,
                uploadReporter = ProgressReporter { _, _ -> }
            ).newBuilder()
                .header("Accept", "text/event-stream")
                .apply { lastEventId?.let { header("Last-Event-ID", it) } }
                .build()
            val listener = object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    lastEventId = id ?: lastEventId
                    trySend(NetworkEvent.SseMessage(type, id, data, retryMillis = null))
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    throwable: Throwable?,
                    response: Response?
                ) {
                    response?.close()
                    if (reconnectAttempts.getAndIncrement() < options.maxReconnectAttempts) {
                        connect()
                    } else {
                        trySend(
                            NetworkEvent.Failure(
                                NetworkFailure.Sse(
                                    message = "SSE 连接失败",
                                    cause = throwable,
                                    requestId = context.requestId
                                )
                            )
                        )
                        close()
                    }
                }
            }
            activeSource.set(EventSources.createFactory(config.okHttpClient).newEventSource(sourceRequest, listener))
        }

        trySend(NetworkEvent.Started)
        connect()
        awaitClose { activeSource.getAndSet(null)?.cancel() }
    }

    private suspend fun <T> ProducerScope<NetworkEvent<T>>.emitOutcome(
        outcome: MiddlewareOutcome
    ) {
        when (outcome) {
            MiddlewareOutcome.Continue -> {
                trySend(NetworkEvent.Failure(NetworkFailure.Middleware("中间件未继续执行请求")))
            }

            MiddlewareOutcome.Cancelled -> trySend(NetworkEvent.Cancelled)
            is MiddlewareOutcome.Failure -> trySend(NetworkEvent.Failure(outcome.error))
            is MiddlewareOutcome.ShortCircuit -> {
                @Suppress("UNCHECKED_CAST")
                trySend(NetworkEvent.Success(outcome.data as T))
            }
        }
    }

    private suspend fun <T> executeTransport(
        request: NetworkRequest<T>,
        context: RequestContext,
        activeCall: AtomicReference<Call?>,
        eventScope: ProducerScope<NetworkEvent<T>>
    ): MiddlewareOutcome = withContext(Dispatchers.IO) {
        val call = config.okHttpClient.newCall(
            buildRequest(
                request = request,
                context = context,
                uploadReporter = ProgressReporter { transferred, total ->
                    eventScope.trySend(
                        NetworkEvent.Progress(
                            direction = NetworkEvent.Progress.Direction.UPLOAD,
                            transferredBytes = transferred,
                            totalBytes = total
                        )
                    )
                }
            )
        )
        activeCall.set(call)
        // 协程取消无法自动中断 OkHttp 的阻塞执行，必须显式取消底层 Call。
        coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                call.cancel()
            }
        }
        try {
            request.timeout?.let { call.timeout().timeout(it.toMillis(), TimeUnit.MILLISECONDS) }
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext MiddlewareOutcome.Failure(httpFailure(response, context.requestId))
                }
                val reporter = ProgressReporter { transferred, total ->
                    eventScope.trySend(
                        NetworkEvent.Progress(
                            direction = NetworkEvent.Progress.Direction.DOWNLOAD,
                            transferredBytes = transferred,
                            totalBytes = total
                        )
                    )
                }
                val data = request.responseDecoder.decode(response, reporter)
                MiddlewareOutcome.ShortCircuit(data)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                MiddlewareOutcome.Cancelled
            } else {
                MiddlewareOutcome.Failure(toFailure(error, context.requestId))
            }
        } finally {
            activeCall.compareAndSet(call, null)
        }
    }

    private fun <T> buildRequest(
        request: NetworkRequest<T>,
        context: RequestContext,
        uploadReporter: ProgressReporter
    ): Request {
        val urlBuilder = requireNotNull(config.baseUrl.toHttpUrl().newBuilder(request.path)) {
            "无法根据基础地址和请求路径构建 URL"
        }
        request.queryParameters.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        val body = if (request.payload == com.creepersan.memorialday.network.transfer.RequestPayload.Empty) {
            null
        } else {
            ProgressRequestBody(
                delegate = payloadCodec.encode(request.payload),
                onProgress = uploadReporter
            )
        }
        return Request.Builder()
            .url(urlBuilder.build())
            .apply { context.headers.forEach(::header) }
            .method(request.method.name, body)
            .build()
    }

    private fun httpFailure(response: Response, requestId: String): NetworkFailure.Http {
        val body = response.body?.string()?.take(MAX_ERROR_BODY_LENGTH)
        return NetworkFailure.Http(
            code = response.code,
            responseBody = body,
            message = "HTTP 请求失败：${response.code}",
            requestId = requestId
        )
    }

    private fun toFailure(error: Throwable, requestId: String?): NetworkFailure {
        return when (error) {
            is SocketTimeoutException -> NetworkFailure.Timeout("网络请求超时", error, requestId)
            is IOException -> NetworkFailure.Connection("网络连接失败", error, requestId)
            is IllegalArgumentException -> NetworkFailure.Configuration(error.message ?: "请求配置非法", error, requestId)
            else -> NetworkFailure.Decode("网络响应处理失败", error, requestId)
        }
    }

    private companion object {
        const val MAX_ERROR_BODY_LENGTH = 4 * 1024
    }
}
