package com.creepersan.memorialday.utils.network

import okhttp3.Interceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** 验证网络请求、响应与取消行为。 */
class NetworkManagerTest {
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
    fun sendsJsonAndReturnsHttpErrorResponse() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("请求错误"))
        val interceptor = Interceptor { chain -> chain.proceed(chain.request().newBuilder().header("Authorization", "token").build()) }
        NetworkManager.addInterceptor(interceptor)
        try {
            val response = NetworkManager.request(NetworkRequest(
                host = server.url("/").toString(), url = "/items", method = NetworkRequestMethod.Post,
                query = mapOf("name" to "中文 & test"), data = NetworkRequestBody.Json(mapOf("title" to "纪念日"))
            )).blockingFirst()
            val recorded = server.takeRequest()
            assertEquals("中文 & test", recorded.requestUrl?.queryParameter("name"))
            assertEquals("token", recorded.getHeader("Authorization"))
            assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))
            assertEquals("{\"title\":\"纪念日\"}", recorded.body.readUtf8())
            assertEquals(400, response.responseCode)
            assertEquals("请求错误", response.bodyString())
        } finally {
            NetworkManager.removeInterceptor(interceptor)
        }
    }

    @Test
    fun sendsFormTextAndBinaryBodies() {
        val cases = listOf(
            NetworkRequestBody.Form(mapOf("name" to "a b")) to "name=a%20b",
            NetworkRequestBody.Text("hello") to "hello",
            NetworkRequestBody.Binary(byteArrayOf(65, 66)) to "AB"
        )
        cases.forEach { (body, expected) ->
            server.enqueue(MockResponse())
            NetworkManager.request(NetworkRequest(server.url("/").toString(), "/", NetworkRequestMethod.Put, data = body)).blockingFirst()
            val recorded = server.takeRequest()
            assertEquals("PUT", recorded.method)
            assertEquals(expected, recorded.body.readUtf8())
        }
    }

    @Test
    fun disposeCancelsInFlightRequest() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val observer = NetworkManager.request(NetworkRequest(server.url("/").toString(), "/")).test()
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        observer.dispose()
        observer.assertNoValues().assertNoErrors().assertNotComplete()
    }

    @Test
    fun invalidRequestReportsError() {
        NetworkManager.request(NetworkRequest(server.url("/").toString(), "/", timeout = 0))
            .test().assertError(IllegalArgumentException::class.java)
    }
}
