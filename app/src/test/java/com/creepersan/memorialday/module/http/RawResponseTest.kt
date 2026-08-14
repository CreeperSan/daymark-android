package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.core.NetworkClientConfig
import com.creepersan.memorialday.module.http.core.NetworkManager
import com.creepersan.memorialday.module.http.codec.NetworkResponse
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.transport.NetworkStream
import com.creepersan.memorialday.module.http.transport.RawNetworkResponse
import com.creepersan.memorialday.module.http.transport.TransportEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 无响应解码器时的原始响应快照测试。
 */
class RawResponseTest {
    /**
     * 验证省略解码器能返回可安全读取的响应快照。
     */
    @Test
    fun requestWithoutDecoderReturnsRawSnapshot() = runTest {
        val manager = NetworkManager(
            NetworkClientConfig("https://example.com"),
            object : TransportEngine {
                override suspend fun execute(
                    request: NetworkRequest<*>,
                    requestBody: RequestBody?,
                    context: RequestContext
                ): RawNetworkResponse = object : RawNetworkResponse {
                    override val statusCode = 204
                    override val headers = mapOf("X-Test" to "ok")
                    override val contentType = "text/plain"
                    override suspend fun bodyText() = "done"
                    override suspend fun bodyBytes() = "done".encodeToByteArray()
                    override fun close() = Unit
                }

                override suspend fun openStream(
                    request: NetworkRequest<*>,
                    requestBody: RequestBody?,
                    context: RequestContext
                ): NetworkStream {
                    error("本测试不需要流式传输")
                }
            }
        )

        val response = manager.request(
            NetworkRequest<NetworkResponse>(path = "/health")
        ).await()

        assertEquals(204, response.statusCode)
        assertEquals("done", response.bodyText())
        assertEquals("ok", response.headers["X-Test"])
        assertEquals(NetworkResponse::class, response::class)
    }
}
