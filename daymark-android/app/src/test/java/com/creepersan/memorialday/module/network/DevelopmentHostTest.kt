package com.creepersan.memorialday.module.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/** 本地 IP 代理必须携带业务域名，才能进入对应的服务路由。 */
class DevelopmentHostTest {
    @Test fun localProxySendsCanonicalHost() {
        val server = MockWebServer().apply { start(); enqueue(MockResponse().setBody("{}")) }
        try {
            val address = server.url("/").toString()
            val headers = DevelopmentHost.headers(address, "https://account.creepersan.com")
            NetworkManager.request(NetworkRequest(address, "/api/user/update", NetworkRequestMethod.Post, data = NetworkRequestBody.Json(mapOf("nickname" to "测试")), headers = headers, authentication = NetworkAuthentication.None)).blockingFirst()
            val request = server.takeRequest(5, TimeUnit.SECONDS)
            assertNotNull(request)
            assertEquals("account.creepersan.com", request!!.getHeader("Host"))
            assertEquals("/api/user/update", request.path)
        } finally { server.shutdown() }
    }

    @Test fun customDomainProxyKeepsItsOwnHost() {
        assertNull(DevelopmentHost.headers("https://proxy.example.com", "https://account.creepersan.com"))
        assertEquals("daymark.creepersan.com", DevelopmentHost.headers("http://10.0.2.2:8080", "https://daymark.creepersan.com")?.get("Host"))
    }
}
