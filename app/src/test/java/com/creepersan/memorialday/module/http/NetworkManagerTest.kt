package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.codec.TextResponseDecoder
import com.creepersan.memorialday.module.http.core.NetworkClientConfig
import com.creepersan.memorialday.module.http.core.NetworkEvent
import com.creepersan.memorialday.module.http.core.NetworkManager
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import com.creepersan.memorialday.module.http.transport.NetworkStream
import com.creepersan.memorialday.module.http.transport.RawNetworkResponse
import com.creepersan.memorialday.module.http.transport.TransportEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 网络客户端冷流和替换传输测试。
 */
class NetworkManagerTest {
    /**
     * 验证未收集请求时不会调用传输层，收集后才返回成功事件。
     */
    @Test
    fun requestIsColdAndUsesInjectedTransport() = runTest {
        val transport = FakeTransport()
        val manager = NetworkManager(
            config = NetworkClientConfig("https://example.com"),
            transport = transport
        )
        val call = manager.request(
            NetworkRequest(
                path = "tags",
                decoder = TextResponseDecoder
            )
        )

        assertEquals(0, transport.executeCount)
        val events = mutableListOf<NetworkEvent<String>>()
        call.events.collect { events += it }

        assertEquals(1, transport.executeCount)
        assertTrue(events.first() is NetworkEvent.Started)
        assertEquals("ok", (events.last() as NetworkEvent.Success).value)
    }

    private class FakeTransport : TransportEngine {
        var executeCount: Int = 0

        override suspend fun execute(
            request: NetworkRequest<*>,
            requestBody: RequestBody?,
            context: RequestContext
        ): RawNetworkResponse {
            executeCount++
            return FakeResponse()
        }

        override suspend fun openStream(
            request: NetworkRequest<*>,
            requestBody: RequestBody?,
            context: RequestContext
        ): NetworkStream {
            error("本测试不需要流式传输")
        }
    }

    private class FakeResponse : RawNetworkResponse {
        override val statusCode: Int = 200
        override val headers: Map<String, String> = emptyMap()
        override val contentType: String? = "text/plain"

        override suspend fun bodyText(): String = "ok"

        override suspend fun bodyBytes(): ByteArray = "ok".encodeToByteArray()

        override fun close() = Unit
    }
}
