package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.core.NetworkClientConfig
import com.creepersan.memorialday.module.http.core.NetworkManager
import com.creepersan.memorialday.module.http.core.NetworkEvent
import com.creepersan.memorialday.module.http.codec.TextResponseDecoder
import com.creepersan.memorialday.module.http.middleware.MiddlewareResult
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.transport.NetworkStream
import com.creepersan.memorialday.module.http.transport.RawNetworkResponse
import com.creepersan.memorialday.module.http.transport.TransportEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 中间件正向和回程顺序测试。
 */
class MiddlewarePipelineTest {
    /**
     * 验证中间件能够挂起调用下游，并在返回时继续执行回程逻辑。
     */
    @Test
    fun middlewareRunsInForwardAndReturnOrder() = runTest {
        val trace = mutableListOf<String>()
        fun middleware(name: String) = NetworkMiddleware { request, context, chain ->
            trace += "$name-before"
            val result = chain.proceed(request, context)
            trace += "$name-after"
            result
        }
        val manager = NetworkManager(
            NetworkClientConfig(
                defaultHost = "https://example.com",
                middlewares = listOf(middleware("global"))
            ),
            object : TransportEngine {
                override suspend fun execute(
                    request: NetworkRequest<*>,
                    requestBody: com.creepersan.memorialday.module.http.request.RequestBody?,
                    context: com.creepersan.memorialday.module.http.middleware.RequestContext
                ): RawNetworkResponse {
                    trace += "transport"
                    return object : RawNetworkResponse {
                        override val statusCode = 200
                        override val headers = emptyMap<String, String>()
                        override val contentType = "text/plain"
                        override suspend fun bodyText() = "ok"
                        override suspend fun bodyBytes() = "ok".encodeToByteArray()
                        override fun close() = Unit
                    }
                }

                override suspend fun openStream(
                    request: NetworkRequest<*>,
                    requestBody: com.creepersan.memorialday.module.http.request.RequestBody?,
                    context: com.creepersan.memorialday.module.http.middleware.RequestContext
                ): NetworkStream {
                    error("本测试不需要流式传输")
                }
            }
        )

        manager.request(
            NetworkRequest(
                path = "test",
                decoder = TextResponseDecoder,
                middlewares = listOf(middleware("request"))
            )
        ).events.collect { _: NetworkEvent<String> -> }

        assertEquals(
            listOf(
                "global-before",
                "request-before",
                "transport",
                "request-after",
                "global-after"
            ),
            trace
        )
    }
}
