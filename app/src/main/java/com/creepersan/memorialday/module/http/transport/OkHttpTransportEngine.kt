package com.creepersan.memorialday.module.http.transport

import com.google.gson.Gson
import com.creepersan.memorialday.module.http.middleware.ProgressDirection
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.HttpMethod
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.BufferedSource
import okio.buffer
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 基于 OkHttp 的传输引擎。
 *
 * OkHttp 只存在于此适配层，核心请求、中间件和 Flow API 不依赖其类型。
 */
class OkHttpTransportEngine(
    private val client: OkHttpClient,
    private val baseHost: String,
    private val gson: Gson = Gson()
) : TransportEngine {

    init {
        require(baseHost.isNotBlank()) { "默认主机不能为空" }
        require(baseHost.startsWith("http://") || baseHost.startsWith("https://")) {
            "默认主机必须使用 http 或 https"
        }
    }

    override suspend fun execute(
        request: NetworkRequest<*>,
        requestBody: RequestBody?,
        context: RequestContext
    ): RawNetworkResponse {
        val call = newCall(request, requestBody, context)
        context.attachCall(call)
        return try {
            OkHttpRawNetworkResponse(awaitResponse(call))
        } finally {
            context.detachCall(call)
        }
    }

    override suspend fun openStream(
        request: NetworkRequest<*>,
        requestBody: RequestBody?,
        context: RequestContext
    ): NetworkStream {
        val call = newCall(request, requestBody, context)
        context.attachCall(call)
        return try {
            val response = awaitResponse(call)
            OkHttpNetworkStream(response, call, context)
        } catch (error: Throwable) {
            context.detachCall(call)
            call.cancel()
            throw error
        }
    }

    private fun newCall(
        request: NetworkRequest<*>,
        requestBody: RequestBody?,
        context: RequestContext
    ): Call {
        val baseUrl = (request.host ?: baseHost).trimEnd('/')
        val path = request.path.trimStart('/')
        val builder = requireNotNull("$baseUrl/$path".toHttpUrlOrNull()) {
            "无法构建合法请求地址"
        }.newBuilder()
        request.query.forEach { (key, value) ->
            value?.let { builder.addQueryParameter(key, it) }
        }

        val body = encodeBody(requestBody, context)
        val okHttpRequest = Request.Builder()
            .url(builder.build())
            .headers(request.headers.toHeaders())
            .method(request.method.name, body)
            .build()
        val call = client.newCall(okHttpRequest)
        request.timeout?.let { duration ->
            call.timeout().timeout(duration.toMillis(), TimeUnit.MILLISECONDS)
        }
        return call
    }

    private fun encodeBody(
        body: RequestBody?,
        context: RequestContext
    ): okhttp3.RequestBody? {
        if (body == null) {
            return null
        }
        val encoded = when (body) {
            is RequestBody.Json -> gson.toJson(body.value)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            is RequestBody.Form -> FormBody.Builder().apply {
                body.fields.forEach { (key, value) -> add(key, value) }
            }.build()

            is RequestBody.Binary -> body.bytes.toRequestBody(body.contentType.toMediaType())
            is RequestBody.File -> File(body.path).asRequestBody(body.contentType.toMediaType())
            is RequestBody.Multipart -> MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .apply {
                    body.parts.forEach { part ->
                        val filePath = part.filePath
                        if (filePath != null) {
                            val file = File(filePath)
                            addFormDataPart(
                                part.name,
                                part.fileName ?: file.name,
                                file.asRequestBody(
                                    (part.contentType ?: "application/octet-stream").toMediaType()
                                )
                            )
                        } else {
                            addFormDataPart(part.name, part.value.orEmpty())
                        }
                    }
                }
                .build()
        }
        return ProgressRequestBody(encoded) { sent, total ->
            context.progressSink?.invoke(ProgressDirection.UPLOAD, sent, total)
        }
    }

    private suspend fun awaitResponse(call: Call): Response =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }
}

/**
 * 带上传进度的 OkHttp 请求体。
 */
private class ProgressRequestBody(
    private val delegate: okhttp3.RequestBody,
    private val onProgress: (Long, Long?) -> Unit
) : okhttp3.RequestBody() {
    override fun contentType() = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: okio.BufferedSink) {
        val total = contentLength().takeIf { it >= 0L }
        var written = 0L
        val progressSink = object : okio.ForwardingSink(sink) {
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onProgress(written, total)
            }
        }
        val bufferedSink = progressSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}

/**
 * OkHttp 响应适配器。
 */
private class OkHttpRawNetworkResponse(
    private val response: Response
) : RawNetworkResponse {
    override val statusCode: Int = response.code
    override val headers: Map<String, String> = response.headers.toMultimap()
        .mapValues { it.value.joinToString(",") }
    override val contentType: String? = response.body?.contentType()?.toString()

    override suspend fun bodyText(): String = withContext(Dispatchers.IO) {
        response.body?.string().orEmpty()
    }

    override suspend fun bodyBytes(): ByteArray = withContext(Dispatchers.IO) {
        response.body?.bytes() ?: ByteArray(0)
    }

    override fun close() {
        response.close()
    }
}

/**
 * OkHttp 流响应适配器。
 */
private class OkHttpNetworkStream(
    private val response: Response,
    private val call: Call,
    private val context: RequestContext
) : NetworkStream {
    private val body: ResponseBody = requireNotNull(response.body) { "响应体为空" }
    override val statusCode: Int = response.code
    override val headers: Map<String, String> = response.headers.toMultimap()
        .mapValues { it.value.joinToString(",") }
    override val contentType: String? = body.contentType()?.toString()
    override val contentLength: Long? = body.contentLength().takeIf { it >= 0L }

    override fun chunks(bufferSize: Int): kotlinx.coroutines.flow.Flow<ByteArray> =
        kotlinx.coroutines.flow.flow {
            require(bufferSize > 0) { "流缓冲区大小必须大于零" }
            val source: BufferedSource = body.source()
            val buffer = ByteArray(bufferSize)
            var transferred = 0L
            while (true) {
                val read = withContext(Dispatchers.IO) { source.read(buffer) }
                if (read == -1) {
                    break
                }
                val chunk = buffer.copyOf(read)
                transferred += read
                context.progressSink?.invoke(ProgressDirection.DOWNLOAD, transferred, contentLength)
                emit(chunk)
            }
        }

    override fun close() {
        context.detachCall(call)
        call.cancel()
        response.close()
    }
}
