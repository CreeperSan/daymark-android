package com.creepersan.memorialday.module.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

/** 检查调试日志能用于定位问题，同时不会输出认证信息。 */
class NetworkLogFormatterTest {
    @Test fun jsonRedactsNestedCredentialsAndKeepsStatus() {
        val result = NetworkLogFormatter.json("""{"code":200,"data":{"nickname":"昵称","refresh_token":"refresh-secret","email":"a@example.com","items":[{"password":"pass-secret","code":"123456"}]}}""", preserveStatusCode = true)
        assertTrue(result.contains("\"code\":200"))
        assertTrue(result.contains("昵称"))
        listOf("refresh-secret", "a@example.com", "pass-secret", "123456").forEach { assertFalse(result.contains(it)) }
        assertFalse(NetworkLogFormatter.json("""{"code":123456}""").contains("123456"))
    }

    @Test fun urlAndNonJsonDoNotExposeSecrets() {
        val result = NetworkLogFormatter.url("https://user:pass@example.com/path?token=secret&count=3".toHttpUrl())
        assertFalse(result.contains("secret")); assertFalse(result.contains("user")); assertFalse(result.contains("pass"))
        assertTrue(result.contains("count=3"))
        assertFalse(NetworkLogFormatter.json("\"secret\"").contains("secret"))
        assertFalse(NetworkLogFormatter.body(NetworkRequestBody.Text("password=secret")).contains("secret"))
    }
}
