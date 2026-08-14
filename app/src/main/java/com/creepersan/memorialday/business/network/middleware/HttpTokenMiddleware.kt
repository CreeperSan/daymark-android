package com.creepersan.memorialday.business.network.middleware

import com.creepersan.memorialday.application.DaymarkApplication
import com.creepersan.memorialday.module.http.middleware.MiddlewareResult
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.http.middleware.NetworkMiddlewareChain
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.store.UserStore
import kotlinx.coroutines.flow.first

object HttpTokenMiddleware : NetworkMiddleware{

    // token 提前失效时间
    private const val TOKEN_OFFSET_MILLISECONDS = 5 * 60 * 1000


    override suspend fun intercept(
        request: NetworkRequest<*>,
        context: RequestContext,
        chain: NetworkMiddlewareChain
    ): MiddlewareResult {
        var token = ""
        var needRefreshToken = false
        val userStore = DaymarkApplication.instance.userStore
        val loginState = userStore.loginState
        if (loginState is UserStore.LoginState.LoginSuccess) {
            val cacheToken = loginState.token
            val cacheTokenExpireTime = loginState.tokenExpireTime
            val cacheRefreshToken = loginState.refreshToken
            val cacheRefreshTokenExpireTime = loginState.refreshTokenExpireTime
            val currentTimestamp = System.currentTimeMillis()
            @Suppress("ControlFlowWithEmptyBody")
            if (cacheToken.isNotBlank() && cacheTokenExpireTime > (currentTimestamp + TOKEN_OFFSET_MILLISECONDS)) {
                // token 可用
                token = cacheToken
            } else if (cacheRefreshToken.isNotBlank() && cacheRefreshTokenExpireTime > ((currentTimestamp + TOKEN_OFFSET_MILLISECONDS))) {
                // refreshToken 可用
                needRefreshToken = true
            } else {
                // token 不可用
            }
        }

        if (needRefreshToken) {
            val responseToken = userStore.actionRefreshToken().first()
            if (responseToken != null) {
                token = responseToken.token
            }
        }

        val headers = request.headers + mapOf(
            "CS-Token" to token,
        )

        return chain.proceed(
            context = context,
            request = request.copy(
                headers = headers,
            ),
        )
    }


}