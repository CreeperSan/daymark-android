package com.creepersan.memorialday.module.network

import io.reactivex.rxjava3.subjects.SingleSubject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 验证鉴权等待、失败传播以及登录和刷新请求的特殊处理。 */
class AuthenticationInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var interceptor: AuthenticationInterceptor

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() { if (::interceptor.isInitialized) NetworkManager.removeInterceptor(interceptor); server.shutdown() }

    @Test fun waitingRequestsShareAuthenticationAndDoNotBlockLogin() {
        val token = SingleSubject.create<String>()
        val waiting = CountDownLatch(5)
        interceptor = AuthenticationInterceptor { waiting.countDown(); token.blockingGet() }
        NetworkManager.addInterceptor(interceptor)
        val observers = (1..5).map { NetworkManager.request(NetworkRequest(server.url("/").toString(), "/protected")).test() }
        assertTrue(waiting.await(5, TimeUnit.SECONDS))
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))

        // 同一主机的业务请求占满调度队列时，登录请求仍须立即发出。
        server.enqueue(MockResponse())
        val login = NetworkManager.request(NetworkRequest(server.url("/").toString(), "/login", authentication = NetworkAuthentication.None)).test()
        val loginRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(loginRequest)
        assertEquals("/login", loginRequest!!.path)
        assertNull(loginRequest.getHeader("CS-Token"))
        login.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors()

        repeat(5) { server.enqueue(MockResponse()) }
        token.onSuccess("new-token")
        observers.forEach { it.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors() }
        repeat(5) { assertEquals("new-token", server.takeRequest(5, TimeUnit.SECONDS)?.getHeader("CS-Token")) }
    }

    @Test fun authenticationFailureDoesNotSendProtectedRequest() {
        interceptor = AuthenticationInterceptor { throw IllegalStateException("expired") }
        NetworkManager.addInterceptor(interceptor)
        NetworkManager.request(NetworkRequest(server.url("/").toString(), "/protected"))
            .test().awaitDone(5, TimeUnit.SECONDS).assertError(java.io.IOException::class.java)
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
    }

    @Test fun refreshUsesOldTokenWithoutWaitingAndAnonymousRequestRemovesToken() {
        interceptor = AuthenticationInterceptor { throw AssertionError("刷新和匿名请求不应等待认证") }
        NetworkManager.addInterceptor(interceptor)
        server.enqueue(MockResponse())
        NetworkManager.request(NetworkRequest(server.url("/").toString(), "/refresh", authentication = NetworkAuthentication.Token("old-token"))).blockingFirst()
        assertEquals("old-token", server.takeRequest().getHeader("CS-Token"))
        server.enqueue(MockResponse())
        NetworkManager.request(NetworkRequest(server.url("/").toString(), "/login", headers = mapOf("CS-Token" to "leak"), authentication = NetworkAuthentication.None)).blockingFirst()
        assertNull(server.takeRequest().getHeader("CS-Token"))
    }
}
