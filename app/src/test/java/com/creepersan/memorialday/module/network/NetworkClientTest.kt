package com.creepersan.memorialday.module.network

import com.creepersan.memorialday.module.network.core.HttpMethod
import com.creepersan.memorialday.module.network.core.NetworkClientBuilder
import com.creepersan.memorialday.module.network.core.NetworkEvent
import com.creepersan.memorialday.module.network.core.NetworkRequest
import com.creepersan.memorialday.module.network.core.NetworkFailure
import com.creepersan.memorialday.module.network.core.NetworkLogger
import com.creepersan.memorialday.module.network.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.network.middleware.MiddlewareOutcome
import com.creepersan.memorialday.module.network.transfer.RequestPayload
import com.creepersan.memorialday.module.network.transfer.ResponseDecoders
import com.google.protobuf.StringValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * 网络客户端的基础行为测试。
 */
class NetworkClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `单请求配置覆盖默认请求头并返回冷流结果`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("完成"))
        val client = NetworkClientBuilder()
            .baseUrl(server.url("/").toString())
            .defaultHeaders(mapOf("Authorization" to "default-token"))
            .build()
        val request = NetworkRequest(
            method = HttpMethod.GET,
            path = "message",
            responseDecoder = ResponseDecoders.text(),
            headers = mapOf("Authorization" to "request-token"),
            queryParameters = mapOf("page" to "1")
        )

        val events = client.execute(request).toList()

        assertTrue(events.first() is NetworkEvent.Started)
        val success = events.filterIsInstance<NetworkEvent.Success<String>>().singleOrNull()
        assertTrue("请求未成功，事件：$events", success != null)
        assertEquals("完成", success?.data)
        val recordedRequest = server.takeRequest()
        assertEquals("request-token", recordedRequest.getHeader("Authorization"))
        assertEquals("/message?page=1", recordedRequest.path)
    }

    @Test
    fun `network logging middleware prints request response details without secrets`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"token":"server-secret","message":"created"}""")
        )
        val logs = mutableListOf<String>()
        val client = NetworkClientBuilder()
            .baseUrl(server.url("/").toString())
            .defaultHeaders(mapOf("Authorization" to "client-secret", "X-Trace" to "trace-1"))
            .logger(NetworkLogger { logs += it })
            .build()

        client.execute(
            NetworkRequest(
                method = HttpMethod.POST,
                path = "login",
                responseDecoder = ResponseDecoders.text(),
                queryParameters = mapOf("token" to "query-secret"),
                payload = RequestPayload.Form(
                    mapOf(
                        "account" to "alice",
                        "password" to "request-secret"
                    )
                )
            )
        ).toList()

        val output = logs.joinToString("\n")
            .replace("authorization", "Authorization")
            .replace("x-trace", "X-Trace")
        assertTrue(output.contains("HTTP REQUEST"))
        assertTrue(output.contains("method=POST"))
        assertTrue(output.contains("headers={Authorization=[***], X-Trace=[trace-1]}"))
        assertTrue(output.contains("params=account=alice&password=***"))
        assertTrue(output.contains("HTTP RESPONSE"))
        assertTrue(output.contains("code=201"))
        assertTrue(output.contains("""result={"token":"***","message":"created"}"""))
        assertTrue(!output.contains("client-secret"))
        assertTrue(!output.contains("query-secret"))
        assertTrue(!output.contains("request-secret"))
        assertTrue(!output.contains("server-secret"))
    }

    @Test
    fun `中间件按照全局再请求级顺序执行并反向回程`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("完成"))
        val trace = mutableListOf<String>()
        val global = NetworkMiddleware { context, chain ->
            trace += "global-before"
            val outcome = chain.proceed(context)
            trace += "global-after"
            outcome
        }
        val requestMiddleware = NetworkMiddleware { context, chain ->
            trace += "request-before"
            val outcome = chain.proceed(context)
            trace += "request-after"
            outcome
        }
        val client = NetworkClientBuilder()
            .baseUrl(server.url("/").toString())
            .globalMiddlewares(listOf(global))
            .build()
        val request = NetworkRequest(
            method = HttpMethod.POST,
            path = "submit",
            responseDecoder = ResponseDecoders.text(),
            payload = RequestPayload.Form(mapOf("name" to "daymark")),
            middlewares = listOf(requestMiddleware)
        )

        client.execute(request).toList()

        assertEquals(
            listOf("global-before", "request-before", "request-after", "global-after"),
            trace
        )
    }

    @Test
    fun `未收集的冷流不会发送请求且短路中间件不会触发传输`() = runTest {
        val client = NetworkClientBuilder().baseUrl(server.url("/").toString()).build()
        val coldRequest = NetworkRequest(
            method = HttpMethod.GET,
            path = "cold",
            responseDecoder = ResponseDecoders.text()
        )
        client.execute(coldRequest)
        assertEquals(null, server.takeRequest(100, TimeUnit.MILLISECONDS))

        val shortcut = NetworkMiddleware { _, _ -> MiddlewareOutcome.ShortCircuit("缓存数据") }
        val events = client.execute(
            coldRequest.copy(middlewares = listOf(shortcut))
        ).toList()
        assertEquals("缓存数据", (events.last() as NetworkEvent.Success).data)
        assertEquals(null, server.takeRequest(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `非成功响应映射为 HTTP 失败`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("维护中"))
        val client = NetworkClientBuilder().baseUrl(server.url("/").toString()).build()
        val events = client.execute(
            NetworkRequest(
                method = HttpMethod.GET,
                path = "unavailable",
                responseDecoder = ResponseDecoders.text()
            )
        ).toList()

        val failure = (events.last() as NetworkEvent.Failure).error
        assertTrue(failure is NetworkFailure.Http)
        assertEquals(503, (failure as NetworkFailure.Http).code)
    }

    @Test
    fun `非法配置和不同客户端均保持隔离`() {
        var thrown = false
        try {
            NetworkClientBuilder().baseUrl("not-a-url").build()
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)

        val first = NetworkClientBuilder()
            .baseUrl(server.url("/").toString())
            .defaultHeaders(mapOf("X-Client" to "first"))
            .build()
        val second = NetworkClientBuilder()
            .baseUrl(server.url("/").toString())
            .defaultHeaders(mapOf("X-Client" to "second"))
            .build()
        server.enqueue(MockResponse().setBody("one"))
        server.enqueue(MockResponse().setBody("two"))
        kotlinx.coroutines.runBlocking {
            first.execute(NetworkRequest(HttpMethod.GET, "one", ResponseDecoders.text())).toList()
            second.execute(NetworkRequest(HttpMethod.GET, "two", ResponseDecoders.text())).toList()
        }
        assertEquals("first", server.takeRequest().getHeader("X-Client"))
        assertEquals("second", server.takeRequest().getHeader("X-Client"))
    }

    @Test
    fun `JSON protobuf 与二进制载荷均可发送`() = runTest {
        val client = NetworkClientBuilder().baseUrl(server.url("/").toString()).build()
        server.enqueue(MockResponse().setBody("ok"))
        server.enqueue(MockResponse().setBody("ok"))
        server.enqueue(MockResponse().setBody("ok"))

        client.execute(
            NetworkRequest(
                HttpMethod.POST,
                "json",
                ResponseDecoders.text(),
                payload = RequestPayload.Json(mapOf("name" to "daymark"))
            )
        ).toList()
        client.execute(
            NetworkRequest(
                HttpMethod.POST,
                "protobuf",
                ResponseDecoders.text(),
                payload = RequestPayload.Protobuf(StringValue.of("daymark"))
            )
        ).toList()
        client.execute(
            NetworkRequest(
                HttpMethod.POST,
                "binary",
                ResponseDecoders.text(),
                payload = RequestPayload.Binary(byteArrayOf(1, 2, 3))
            )
        ).toList()

        assertTrue(server.takeRequest().body.readUtf8().contains("daymark"))
        assertEquals("application/x-protobuf", server.takeRequest().getHeader("Content-Type"))
        assertEquals(3L, server.takeRequest().body.size)
    }

    @Test
    fun `断点下载校验范围响应并原子提交文件`() = runTest {
        val directory = createTempDirectory("network-download").toFile()
        val target = File(directory, "archive.bin")
        val temporary = File(directory, "archive.bin.download")
        temporary.writeText("abc")
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 3-5/6")
                .setBody("def")
        )
        val client = NetworkClientBuilder().baseUrl(server.url("/").toString()).build()

        val events = client.download("archive", target).toList()

        assertEquals("abcdef", target.readText())
        assertTrue(!temporary.exists())
        assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
        assertTrue(events.last() is NetworkEvent.Success)
    }

    @Test
    fun `SSE 消息按服务端顺序提供`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("id: event-1\nevent: update\ndata: completed\n\n")
        )
        val client = NetworkClientBuilder().baseUrl(server.url("/").toString()).build()
        val event = client.sse(
            NetworkRequest(
                method = HttpMethod.GET,
                path = "events",
                responseDecoder = ResponseDecoders.text()
            )
        ).filterIsInstance<NetworkEvent.SseMessage>().first()

        assertEquals("event-1", event.id)
        assertEquals("update", event.event)
        assertEquals("completed", event.data)
    }
}
