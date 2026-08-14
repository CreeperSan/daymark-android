package com.creepersan.memorialday.business.network.middleware

import com.creepersan.memorialday.application.DaymarkApplication
import com.creepersan.memorialday.module.http.middleware.MiddlewareResult
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import com.creepersan.memorialday.module.http.middleware.NetworkMiddlewareChain
import com.creepersan.memorialday.module.http.middleware.RequestContext
import com.creepersan.memorialday.module.http.request.NetworkRequest

object HttpHeaderInfoMiddleware : NetworkMiddleware {

    override suspend fun intercept(
        request: NetworkRequest<*>,
        context: RequestContext,
        chain: NetworkMiddlewareChain
    ): MiddlewareResult {
        val deviceInfoStore = DaymarkApplication.instance.deviceInfoStore
        val appStateStore = DaymarkApplication.instance.appStateStore

        val headers = request.headers + mapOf(
            "CS-App" to appStateStore.app,
            "CS-Version" to appStateStore.versionCode.toString(),
            "CS-Device-Platform" to deviceInfoStore.devicePlatform,
            "CS-Device-Code" to deviceInfoStore.deviceCode,
            "CS-Device-Name" to deviceInfoStore.deviceName,
        )

        return chain.proceed(
            context = context,
            request = request.copy(
                headers = headers,
            ),
        )
    }

}