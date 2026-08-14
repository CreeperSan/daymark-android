package com.creepersan.memorialday.business.network.middleware

import android.os.SystemClock
import com.creepersan.memorialday.application.DaymarkApplication
import com.creepersan.memorialday.module.http.core.NetworkError
import com.creepersan.memorialday.module.http.middleware.MiddlewareResult
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.http.middleware.NetworkMiddlewareChain
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.store.UserStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.plus

object HttpTokenMiddleware : NetworkMiddleware {

    // token 提前失效时间
    private const val TOKEN_OFFSET_MILLISECONDS = 5 * 60 * 1000
    private const val REFRESH_COOLDOWN_MILLISECONDS = 3 * 1000L

    /**
     * 刷新锁只保护刷新流程，不阻塞普通 Token 注入。
     */
    private val refreshMutex = Mutex()

    /**
     * 使用单调时钟记录刷新尝试时间，避免系统时间变化影响频率限制。
     */
    private var lastRefreshAttemptElapsedRealtime = Long.MIN_VALUE


    override suspend fun intercept(
        request: NetworkRequest<*>,
        context: RequestContext,
        chain: NetworkMiddlewareChain
    ): MiddlewareResult {
        val userStore = DaymarkApplication.instance.userStore

        val loginState = userStore.loginState
        if (loginState !is UserStore.LoginState.LoginSuccess) {
            // 未登录请求不注入空 Token，保留登录等公开接口的正常行为。
            return chain.proceed(request = request, context = context)
        }

        val tokenResult = getTokenForRequest(
            userStore = userStore,
            requestId = context.requestId
        )
        if (tokenResult is TokenResult.Failure) {
            return MiddlewareResult.Failure(tokenResult.error)
        }

        return chain.proceed(
            context = context,
            request = request.withToken((tokenResult as TokenResult.Success).token)
        )
    }

    /**
     * 获取当前请求可以使用的 Token。
     *
     * 只有发现本地 Token 失效时才进入刷新锁；进入锁后会再次读取登录状态，
     * 避免并发请求在前一个请求刷新成功后重复刷新。
     */
    private suspend fun getTokenForRequest(userStore: UserStore, requestId: String): TokenResult {
        val loginState = userStore.loginState as? UserStore.LoginState.LoginSuccess ?: return TokenResult.Failure(
            authenticationError(
                reason = "登录状态已失效",
                requestId = requestId
            )
        )

        if (isTokenValid(loginState)) {
            return TokenResult.Success(loginState.token)
        }

        return refreshTokenIfNeeded(
            userStore = userStore,
            requestId = requestId
        )
    }

    /**
     * 在全局刷新锁内完成 Token 二次检查、频率限制和刷新操作。
     */
    private suspend fun refreshTokenIfNeeded(userStore: UserStore, requestId: String): TokenResult {
        return refreshMutex.withLock {
            val latestState = userStore.loginState as? UserStore.LoginState.LoginSuccess ?: return@withLock TokenResult.Failure(
                authenticationError(
                    reason = "登录状态已失效",
                    requestId = requestId
                )
            )

            // 其他并发请求可能已经完成刷新，此时直接复用最新 Token。
            if (isTokenValid(latestState)) {
                return@withLock TokenResult.Success(latestState.token)
            }

            if (!isRefreshTokenValid(latestState)) {
                return@withLock TokenResult.Failure(
                    authenticationError(
                        reason = "刷新 Token 已失效",
                        requestId = requestId
                    )
                )
            }

            val now = SystemClock.elapsedRealtime()
            if (lastRefreshAttemptElapsedRealtime != Long.MIN_VALUE && now - lastRefreshAttemptElapsedRealtime < REFRESH_COOLDOWN_MILLISECONDS) {
                return@withLock TokenResult.Failure(
                    authenticationError(
                        reason = "Token 刷新过于频繁，请稍后重试",
                        requestId = requestId
                    )
                )
            }

            // 在真正发起刷新前记录时间，刷新失败同样进入冷却期。
            lastRefreshAttemptElapsedRealtime = now
            when (val refreshState = userStore.actionRefreshToken().first()) {
                is UserStore.TokenRefreshState.Success -> {
                    val refreshedToken = refreshState.token.token
                    if (refreshedToken.isBlank()) {
                        TokenResult.Failure(
                            authenticationError(
                                reason = "刷新 Token 为空",
                                requestId = requestId
                            )
                        )
                    } else {
                        TokenResult.Success(refreshedToken)
                    }
                }

                UserStore.TokenRefreshState.Fail -> TokenResult.Failure(
                    authenticationError(
                        reason = "Token 刷新失败",
                        requestId = requestId
                    )
                )

                UserStore.TokenRefreshState.TokenInvalid -> TokenResult.Failure(
                    authenticationError(
                        reason = "刷新 Token 已失效",
                        requestId = requestId
                    )
                )
            }
        }
    }

    /**
     * 判断访问 Token 是否在安全时间窗口内有效。
     */
    private fun isTokenValid(loginState: UserStore.LoginState.LoginSuccess): Boolean {
        return loginState.token.isNotBlank() && loginState.tokenExpireTime > System.currentTimeMillis() + TOKEN_OFFSET_MILLISECONDS
    }

    /**
     * 判断刷新 Token 是否仍可用于刷新。
     */
    private fun isRefreshTokenValid(loginState: UserStore.LoginState.LoginSuccess): Boolean {
        return loginState.refreshToken.isNotBlank() && loginState.refreshTokenExpireTime > System.currentTimeMillis() + TOKEN_OFFSET_MILLISECONDS
    }

    /**
     * 构造不包含敏感 Token 的认证失败结果。
     */
    private fun authenticationError(reason: String, requestId: String): NetworkError.Business {
        return NetworkError.Business(
            code = "TOKEN_REFRESH_FAILED",
            reason = reason,
            requestId = requestId
        )
    }

    /**
     * 返回带认证 Header 的不可变请求。
     */
    private fun NetworkRequest<*>.withToken(token: String): NetworkRequest<*> {
        return copy(
            headers = headers + ("CS-Token" to token)
        )
    }

    /**
     * Token 获取结果。
     */
    private sealed interface TokenResult {
        data class Success(val token: String) : TokenResult

        data class Failure(val error: NetworkError) : TokenResult
    }
}