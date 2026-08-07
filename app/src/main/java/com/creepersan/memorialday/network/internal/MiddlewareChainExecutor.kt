package com.creepersan.memorialday.network.internal

import com.creepersan.memorialday.network.middleware.MiddlewareOutcome
import com.creepersan.memorialday.network.middleware.NetworkMiddleware
import com.creepersan.memorialday.network.middleware.NetworkMiddlewareChain
import com.creepersan.memorialday.network.middleware.RequestContext

/**
 * 以注册顺序执行中间件的内部实现。
 *
 * 递归返回值天然使调用 [proceed] 之后的代码以相反顺序执行，满足响应回程顺序要求。
 */
internal class MiddlewareChainExecutor(
    private val middlewares: List<NetworkMiddleware>,
    private val terminal: suspend (RequestContext) -> MiddlewareOutcome
) {
    suspend fun proceed(context: RequestContext): MiddlewareOutcome = invoke(index = 0, context = context)

    private suspend fun invoke(index: Int, context: RequestContext): MiddlewareOutcome {
        if (index == middlewares.size) {
            return terminal(context)
        }

        val chain = NetworkMiddlewareChain { nextContext ->
            invoke(index + 1, nextContext)
        }
        return when (val outcome = middlewares[index].intercept(context, chain)) {
            MiddlewareOutcome.Continue -> invoke(index + 1, context)
            else -> outcome
        }
    }
}
