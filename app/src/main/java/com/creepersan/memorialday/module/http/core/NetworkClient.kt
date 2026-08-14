package com.creepersan.memorialday.module.http.core

import com.creepersan.memorialday.module.http.middleware.MiddlewareRegistry
import com.creepersan.memorialday.module.http.middleware.MiddlewareResult
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.http.middleware.NetworkMiddlewareChain
import com.creepersan.memorialday.module.http.middleware.ProgressDirection
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.codec.NetworkResponse
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.sse.ReconnectPolicy
import com.creepersan.memorialday.module.http.sse.SseCall
import com.creepersan.memorialday.module.http.sse.SseEvent
import com.creepersan.memorialday.module.http.sse.SseEventDecoder
import com.creepersan.memorialday.module.http.sse.SseOptions
import com.creepersan.memorialday.module.http.sse.SseParser
import com.creepersan.memorialday.module.http.stream.ByteStreamCall
import com.creepersan.memorialday.module.http.stream.ByteStreamEvent
import com.creepersan.memorialday.module.http.stream.DownloadResult
import com.creepersan.memorialday.module.http.stream.ProgressSampler
import com.creepersan.memorialday.module.http.transport.NetworkStream
import com.creepersan.memorialday.module.http.transport.TransportEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File

/**
 * 网络客户端配置。
 */
data class NetworkClientConfig(
    val defaultHost: String,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val middlewares: List<NetworkMiddleware> = emptyList(),
    val logger: NetworkLogger = NetworkLogger.Noop
) {
    init {
        require(defaultHost.startsWith("http://") || defaultHost.startsWith("https://")) {
            "默认主机必须使用 http 或 https"
        }
    }
}

/**
 * 新网络框架的主要入口。
 *
 * 客户端线程安全；全局中间件在每次请求开始时快照，执行中的请求不受后续注册影响。
 */
class NetworkManager(
    private val config: NetworkClientConfig,
    private val transport: TransportEngine,
    private val registry: MiddlewareRegistry = MiddlewareRegistry()
) {
    /**
     * 注册全局中间件。
     */
    fun registerMiddleware(middleware: NetworkMiddleware) {
        registry.register(middleware)
    }

    /**
     * 移除全局中间件。
     */
    fun unregisterMiddleware(middleware: NetworkMiddleware) {
        registry.unregister(middleware)
    }

    /**
     * 创建普通请求调用。
     *
     * 返回冷 Flow，调用方不收集时不会执行中间件或建立连接。
     */
    fun <T> request(request: NetworkRequest<T>): RequestCall<T> {
        return RequestCallImpl(source = { execute(request.snapshot()) })
    }

    /**
     * 创建原始字节流调用。
     */
    fun stream(request: NetworkRequest<*>): ByteStreamCall {
        return ByteStreamCallImpl(source = { executeStream(request.snapshot()) })
    }

    /**
     * 创建 SSE 长连接调用。
     *
     * SSE 使用独立的重连策略；取消 Flow 会停止重连并关闭当前响应。
     */
    fun <T> sse(
        request: NetworkRequest<*>,
        decoder: SseEventDecoder<T>,
        options: SseOptions = SseOptions()
    ): SseCall<T> {
        return SseCallImpl(source = { executeSse(request.snapshot(), decoder, options) })
    }

    /**
     * 将响应流安全下载到文件。
     *
     * 数据先写入目标同目录的临时文件，成功后再原子移动；启用恢复时会校验
     * 服务端是否返回 206，不支持范围请求时自动从头开始。
     */
    suspend fun downloadTo(
        request: NetworkRequest<*>,
        target: File,
        resume: Boolean = true
    ): DownloadResult {
        require(target.parentFile?.exists() == true) { "目标文件目录不存在" }
        val temporary = File(target.parentFile, "${target.name}.part")
        val existingBytes = if (resume) temporary.length() else 0L
        val headers = if (existingBytes > 0L) {
            request.headers + ("Range" to "bytes=$existingBytes-")
        } else {
            request.headers
        }
        val effectiveRequest = request.copy(headers = headers)
        var restarted = false
        var outputBytes = if (existingBytes > 0L) existingBytes else 0L
        FileOutputStream(temporary, existingBytes > 0L).use { output ->
            stream(effectiveRequest).events.collect { event ->
                when (event) {
                    is ByteStreamEvent.Started -> {
                        if (existingBytes > 0L && event.statusCode != 206) {
                            output.close()
                            temporary.delete()
                            restarted = true
                        }
                    }

                    is ByteStreamEvent.Chunk -> {
                        if (!restarted) {
                            output.write(event.bytes)
                            outputBytes += event.bytes.size
                        }
                    }

                    is ByteStreamEvent.Failed -> throw NetworkException(event.error)
                    ByteStreamEvent.Cancelled -> throw NetworkException(
                        NetworkError.Cancelled()
                    )
                    is ByteStreamEvent.Progress,
                    ByteStreamEvent.Completed -> Unit
                }
            }
        }
        if (restarted) {
            return downloadTo(request, target, resume = false)
        }
        Files.move(
            temporary.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
        return DownloadResult(target.absolutePath, outputBytes, existingBytes > 0L)
    }

    private fun <T> execute(request: NetworkRequest<T>): Flow<NetworkEvent<T>> = channelFlow {
        val context = RequestContext()
        val effectiveRequest = request.copy(
            host = request.host ?: config.defaultHost,
            headers = config.defaultHeaders + request.headers
        )
        val uploadSampler = ProgressSampler(request.progressConfig) { transferred, total ->
            trySend(
                NetworkEvent.Progress(
                    NetworkEvent.Progress.Direction.UPLOAD,
                    transferred,
                    total
                )
            )
        }
        val downloadSampler = ProgressSampler(request.progressConfig) { transferred, total ->
            trySend(
                NetworkEvent.Progress(
                    NetworkEvent.Progress.Direction.DOWNLOAD,
                    transferred,
                    total
                )
            )
        }
        context.progressSink = { direction, transferred, total ->
            when (direction) {
                ProgressDirection.UPLOAD -> uploadSampler.report(transferred, total)
                ProgressDirection.DOWNLOAD -> downloadSampler.report(transferred, total)
            }
        }
        try {
            config.logger.log(
                "网络请求开始 requestId=${context.requestId} method=${effectiveRequest.method}"
            )
            send(NetworkEvent.Started(context.requestId))
            val result = executeThroughMiddleware(effectiveRequest, context) { current ->
                transport.execute(current, current.body, context).use { response ->
                    if (response.statusCode !in 200..299) {
                        MiddlewareResult.Failure(
                            NetworkError.Http(
                                statusCode = response.statusCode,
                                bodySummary = response.bodyText().take(MAX_ERROR_SUMMARY_LENGTH),
                                requestId = context.requestId
                            )
                        )
                    } else {
                        try {
                            val decoder = current.decoder
                            if (decoder != null) {
                                MiddlewareResult.Success(decoder.decode(response))
                            } else {
                                MiddlewareResult.Success(
                                    NetworkResponse(
                                        statusCode = response.statusCode,
                                        headers = response.headers,
                                        contentType = response.contentType,
                                        body = response.bodyBytes()
                                    )
                                )
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            MiddlewareResult.Failure(
                                NetworkError.Decode(
                                    reason = "响应解码失败",
                                    requestId = context.requestId,
                                    cause = error
                                )
                            )
                        }
                    }
                }
            }
            emitResult(result, context.requestId)
            config.logger.log("网络请求结束 requestId=${context.requestId}")
        } catch (error: CancellationException) {
            context.cancelCall()
            trySend(NetworkEvent.Cancelled)
            throw error
        } catch (error: Throwable) {
            trySend(NetworkEvent.Failure(mapError(error, context.requestId)))
        } finally {
            context.progressSink = null
            context.cancelCall()
        }
    }

    private suspend fun <T> kotlinx.coroutines.channels.ProducerScope<NetworkEvent<T>>.emitResult(
        result: MiddlewareResult,
        requestId: String
    ) {
        when (result) {
            is MiddlewareResult.Success -> {
                @Suppress("UNCHECKED_CAST")
                send(NetworkEvent.Success(result.value as T))
            }

            is MiddlewareResult.Failure -> send(NetworkEvent.Failure(result.error))
            MiddlewareResult.Cancelled -> send(NetworkEvent.Cancelled)
            is MiddlewareResult.Proceed<*> -> send(
                NetworkEvent.Failure(
                    NetworkError.Configuration("中间件未完成请求", requestId)
                )
            )
        }
    }

    private suspend fun executeThroughMiddleware(
        initialRequest: NetworkRequest<*>,
        context: RequestContext,
        terminal: suspend (NetworkRequest<*>) -> MiddlewareResult
    ): MiddlewareResult {
        val middlewares = config.middlewares + registry.snapshot() + initialRequest.middlewares

        suspend fun proceed(index: Int, request: NetworkRequest<*>): MiddlewareResult {
            if (index == middlewares.size) {
                return terminal(request)
            }
            return middlewares[index].intercept(
                request = request,
                context = context,
                chain = NetworkMiddlewareChain { nextRequest, _ ->
                    proceed(index + 1, nextRequest)
                }
            )
        }

        return proceed(0, initialRequest)
    }

    private fun executeStream(request: NetworkRequest<*>): Flow<ByteStreamEvent> = channelFlow {
        val context = RequestContext()
        val effectiveRequest = request.copy(
            host = request.host ?: config.defaultHost,
            headers = config.defaultHeaders + request.headers
        )
        val downloadSampler = ProgressSampler(request.progressConfig) { transferred, total ->
            trySend(ByteStreamEvent.Progress(transferred, total))
        }
        context.progressSink = { direction, transferred, total ->
            if (direction == ProgressDirection.DOWNLOAD) {
                downloadSampler.report(transferred, total)
            }
        }
        var stream: NetworkStream? = null
        try {
            val result = executeThroughMiddleware(effectiveRequest, context) { current ->
                stream = transport.openStream(current, current.body, context)
                val statusCode = stream?.statusCode ?: -1
                if (statusCode !in 200..299) {
                    stream?.close()
                    MiddlewareResult.Failure(
                        NetworkError.Http(statusCode, requestId = context.requestId)
                    )
                } else {
                    MiddlewareResult.Success(stream)
                }
            }
            when (result) {
                is MiddlewareResult.Success -> {
                    val activeStream = result.value as? NetworkStream
                        ?: error("流式传输未返回 NetworkStream")
                    send(
                        ByteStreamEvent.Started(
                            context.requestId,
                            activeStream.statusCode,
                            activeStream.headers
                        )
                    )
                    var offset = 0L
                    activeStream.chunks().collect { bytes ->
                        send(ByteStreamEvent.Chunk(bytes, offset))
                        offset += bytes.size
                    }
                    send(ByteStreamEvent.Completed)
                }

                is MiddlewareResult.Failure -> send(ByteStreamEvent.Failed(result.error))
                MiddlewareResult.Cancelled -> send(ByteStreamEvent.Cancelled)
                is MiddlewareResult.Proceed<*> -> send(
                    ByteStreamEvent.Failed(NetworkError.Configuration("中间件未完成流式请求"))
                )
            }
        } catch (error: CancellationException) {
            context.cancelCall()
            trySend(ByteStreamEvent.Cancelled)
            throw error
        } catch (error: Throwable) {
            trySend(ByteStreamEvent.Failed(mapError(error, context.requestId)))
        } finally {
            context.progressSink = null
            stream?.close()
            context.cancelCall()
        }
    }

    private fun <T> executeSse(
        request: NetworkRequest<*>,
        decoder: SseEventDecoder<T>,
        options: SseOptions
    ): Flow<SseEvent<T>> = flow {
        val context = RequestContext()
        var lastEventId = options.initialLastEventId
        var reconnectAttempt = 0
        var completed = false

        while (!completed) {
            var activeStream: NetworkStream? = null
            val currentRequest = request.copy(
                host = request.host ?: config.defaultHost,
                headers = buildMap {
                    putAll(config.defaultHeaders)
                    putAll(request.headers)
                    put("Accept", "text/event-stream")
                    lastEventId?.let { put("Last-Event-ID", it) }
                }
            )
            try {
                val result = executeThroughMiddleware(currentRequest, context) { current ->
                    activeStream = transport.openStream(current, current.body, context)
                    val statusCode = activeStream?.statusCode ?: -1
                    if (statusCode !in 200..299) {
                        activeStream?.close()
                        MiddlewareResult.Failure(
                            NetworkError.Sse(
                                reason = "SSE 握手失败：HTTP $statusCode",
                                retryable = statusCode in 500..599,
                                requestId = context.requestId
                            )
                        )
                    } else {
                        MiddlewareResult.Success(activeStream)
                    }
                }
                when (result) {
                    is MiddlewareResult.Success -> {
                        val stream = result.value as? NetworkStream
                            ?: error("SSE 传输未返回 NetworkStream")
                        emit(SseEvent.Started(context.requestId, stream.statusCode, stream.headers))
                        val parser = SseParser()
                        stream.chunks().collect { bytes ->
                            parser.feed(bytes).forEach { raw ->
                                emitDecoded(raw, decoder, context) { id ->
                                    lastEventId = id ?: lastEventId
                                }
                            }
                            if (parser.consumeHeartbeat()) {
                                emit(SseEvent.Heartbeat)
                            }
                        }
                        parser.finish().forEach { raw ->
                            emitDecoded(raw, decoder, context) { id ->
                                lastEventId = id ?: lastEventId
                            }
                        }
                        completed = true
                        emit(SseEvent.Completed)
                    }

                    is MiddlewareResult.Failure -> {
                        if (canReconnect(result.error, options.reconnectPolicy, reconnectAttempt)) {
                            reconnectAttempt++
                            context.reconnectAttempts = reconnectAttempt
                            delay(reconnectDelay(options.reconnectPolicy, reconnectAttempt))
                        } else {
                            emit(SseEvent.Failed(result.error))
                            completed = true
                        }
                    }

                    MiddlewareResult.Cancelled -> {
                        emit(SseEvent.Cancelled)
                        completed = true
                    }

                    is MiddlewareResult.Proceed<*> -> {
                        emit(SseEvent.Failed(NetworkError.Sse("中间件未完成 SSE 请求")))
                        completed = true
                    }
                }
            } catch (error: CancellationException) {
                context.cancelCall()
                emit(SseEvent.Cancelled)
                throw error
            } catch (error: Throwable) {
                val mapped = mapError(error, context.requestId)
                if (canReconnect(mapped, options.reconnectPolicy, reconnectAttempt)) {
                    reconnectAttempt++
                    context.reconnectAttempts = reconnectAttempt
                    delay(reconnectDelay(options.reconnectPolicy, reconnectAttempt))
                } else {
                    emit(SseEvent.Failed(mapped))
                    completed = true
                }
            } finally {
                activeStream?.close()
                context.cancelCall()
            }
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<SseEvent<T>>.emitDecoded(
        raw: com.creepersan.memorialday.module.http.sse.RawSseEvent,
        decoder: SseEventDecoder<T>,
        context: RequestContext,
        onId: (String?) -> Unit
    ) {
        val value = decoder.decode(raw)
        onId(raw.id)
        context.lastEventId = raw.id ?: context.lastEventId
        emit(SseEvent.Message(raw.event, raw.id, raw.retryMillis, value))
    }

    private fun canReconnect(
        error: NetworkError,
        policy: ReconnectPolicy,
        attempt: Int
    ): Boolean {
        return policy is ReconnectPolicy.Limited &&
            attempt < policy.maxAttempts &&
            (error is NetworkError.Connection ||
                error is NetworkError.Timeout ||
                (error is NetworkError.Sse && error.retryable))
    }

    private fun reconnectDelay(policy: ReconnectPolicy, attempt: Int): Long {
        if (policy !is ReconnectPolicy.Limited) {
            return 0L
        }
        val multiplier = 1L shl (attempt - 1).coerceAtMost(30)
        return (policy.initialDelayMillis * multiplier).coerceAtMost(policy.maxDelayMillis)
    }

    private fun mapError(error: Throwable, requestId: String): NetworkError {
        return when (error) {
            is SocketTimeoutException -> NetworkError.Timeout("网络请求超时", requestId, error)
            is IOException -> NetworkError.Connection("网络连接失败", requestId, error)
            is IllegalArgumentException -> NetworkError.Configuration(
                error.message.orEmpty(),
                requestId,
                error
            )
            else -> NetworkError.Decode("网络执行失败", requestId, error)
        }
    }

    private companion object {
        const val MAX_ERROR_SUMMARY_LENGTH = 1024
    }
}

/**
 * 普通请求调用接口。
 */
interface RequestCall<T> {
    /**
     * 请求事件冷流。
     */
    val events: Flow<NetworkEvent<T>>

    /**
     * 只暴露普通请求的业务结果流，自动过滤开始、进度等控制事件。
     *
     * 失败会抛出 [NetworkException]，取消会抛出协程取消异常。
     */
    val result: Flow<T>

    /**
     * 等待成功结果。
     */
    suspend fun await(): T

    /**
     * 在作用域中注册成功回调。
     */
    fun then(scope: CoroutineScope, onSuccess: suspend (T) -> Unit): RequestCall<T>

    /**
     * 注册失败回调。
     */
    fun catch(onFailure: suspend (NetworkError) -> Unit): RequestCall<T>

    /**
     * 对成功业务结果进行转换。
     */
    fun <R> map(transform: suspend (T) -> R): Flow<R> = result.map(transform)

    /**
     * 对成功业务结果执行串行异步展开。
     */
    fun <R> flatMapConcat(transform: suspend (T) -> Flow<R>): Flow<R> =
        result.flatMapConcat(transform)
}

private class RequestCallImpl<T>(
    private val source: () -> Flow<NetworkEvent<T>>,
    private val failureHandler: (suspend (NetworkError) -> Unit)? = null
) : RequestCall<T> {
    override val events: Flow<NetworkEvent<T>>
        get() = source()

    override val result: Flow<T>
        get() = events.transform { event ->
            when (event) {
                is NetworkEvent.Success -> emit(event.value)
                is NetworkEvent.Failure -> throw NetworkException(event.error)
                NetworkEvent.Cancelled -> throw CancellationException("网络请求已取消")
                is NetworkEvent.Started,
                is NetworkEvent.Progress -> Unit
            }
        }

    override suspend fun await(): T {
        var result: T? = null
        var failure: NetworkError? = null
        source().collect { event ->
            when (event) {
                is NetworkEvent.Success -> result = event.value
                is NetworkEvent.Failure -> failure = event.error
                NetworkEvent.Cancelled -> failure = NetworkError.Cancelled()
                else -> Unit
            }
        }
        failure?.let { throw NetworkException(it) }
        return result ?: throw NetworkException(NetworkError.Decode("请求未产生成功结果"))
    }

    override fun then(scope: CoroutineScope, onSuccess: suspend (T) -> Unit): RequestCall<T> {
        scope.launch {
            source().collect { event ->
                if (event is NetworkEvent.Success) {
                    onSuccess(event.value)
                } else if (event is NetworkEvent.Failure) {
                    failureHandler?.invoke(event.error)
                }
            }
        }
        return this
    }

    override fun catch(onFailure: suspend (NetworkError) -> Unit): RequestCall<T> {
        return RequestCallImpl(source, onFailure)
    }
}

/**
 * await 调用失败时抛出的异常。
 */
class NetworkException(val error: NetworkError) : IOException(error.toString())

private class ByteStreamCallImpl(
    private val source: () -> Flow<ByteStreamEvent>
) : ByteStreamCall {
    override val events: Flow<ByteStreamEvent>
        get() = source()
}

private class SseCallImpl<T>(
    private val source: () -> Flow<SseEvent<T>>
) : SseCall<T> {
    override val events: Flow<SseEvent<T>>
        get() = source()
}

private fun ProgressDirection.toEventDirection(): NetworkEvent.Progress.Direction {
    return when (this) {
        ProgressDirection.UPLOAD -> NetworkEvent.Progress.Direction.UPLOAD
        ProgressDirection.DOWNLOAD -> NetworkEvent.Progress.Direction.DOWNLOAD
    }
}
