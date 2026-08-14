package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.codec.TextResponseDecoder
import com.creepersan.memorialday.module.http.core.NetworkClientConfig
import com.creepersan.memorialday.module.http.core.NetworkManager
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import com.creepersan.memorialday.module.http.transport.NetworkStream
import com.creepersan.memorialday.module.http.transport.RawNetworkResponse
import com.creepersan.memorialday.module.http.transport.TransportEngine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 普通请求业务结果 Flow 测试。
 */
class RequestResultFlowTest {
    /**
     * 验证 result 和 map/flatMapConcat 只处理成功业务数据。
     */
    @Test
    fun resultFlowSupportsMapAndFlatMapConcat() = runTest {
        val manager = NetworkManager(
            NetworkClientConfig("https://example.com"),
            FixedTransport()
        )
        val call = manager.request(
            NetworkRequest<String>(
                path = "/value",
                decoder = TextResponseDecoder
            )
        )

        val values = call
            .map { it.length }
            .let { lengths ->
                lengths
                    .flatMapConcat { flowOf(it * 2) }
                    .toList()
            }

        assertEquals(listOf(4), values)
    }

    private class FixedTransport : TransportEngine {
        override suspend fun execute(
            request: NetworkRequest<*>,
            requestBody: RequestBody?,
            context: RequestContext
        ): RawNetworkResponse = object : RawNetworkResponse {
            override val statusCode = 200
            override val headers = emptyMap<String, String>()
            override val contentType = "text/plain"
            override suspend fun bodyText() = "ok"
            override suspend fun bodyBytes() = "ok".encodeToByteArray()
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
}
