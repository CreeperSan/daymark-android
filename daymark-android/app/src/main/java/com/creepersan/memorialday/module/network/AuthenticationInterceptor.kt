package com.creepersan.memorialday.module.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** 业务请求等待可用令牌；登录无需令牌，刷新使用原令牌且不递归刷新。 */
class AuthenticationInterceptor(private val getToken: () -> String) : Interceptor {
    companion object {
        private const val TOKEN_HEADER = "CS-Token"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authentication = request.tag(NetworkAuthentication::class.java) ?: NetworkAuthentication.Required
        val token = when (authentication) {
            NetworkAuthentication.None -> null
            is NetworkAuthentication.Token -> authentication.token
            NetworkAuthentication.Required -> try {
                getToken().also { require(it.isNotBlank()) { "Empty authentication token" } }
            } catch (error: Exception) {
                throw IOException("Authentication failed", error)
            }
        }
        // 不信任调用方手动传入的认证头，避免无认证请求意外泄露令牌。
        val builder = request.newBuilder().removeHeader(TOKEN_HEADER)
        if (token != null) builder.header(TOKEN_HEADER, token)
        return chain.proceed(builder.build())
    }
}
