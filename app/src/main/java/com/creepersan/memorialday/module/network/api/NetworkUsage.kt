package com.creepersan.memorialday.module.network.api

import com.creepersan.memorialday.module.network.core.HttpMethod
import com.creepersan.memorialday.module.network.core.NetworkClientBuilder
import com.creepersan.memorialday.module.network.core.NetworkEvent
import com.creepersan.memorialday.module.network.core.NetworkLogger
import com.creepersan.memorialday.module.network.core.NetworkRequest
import com.creepersan.memorialday.module.network.transfer.ResponseDecoders
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * 网络框架的最小接入示例。
 *
 * 业务层在自己的协程作用域内收集返回的 Flow；取消该作用域会同时取消底层请求。
 */
object NetworkUsage {
    /**
     * 创建一个可复用的业务客户端。
     *
     * 实际项目应将地址与认证中间件由环境配置和业务模块提供。
     */
    fun createClient(baseUrl: String) = NetworkClientBuilder()
        .baseUrl(baseUrl)
        .logger(NetworkLogger.LOGCAT)
        .build()

    /**
     * 仅保留成功数据的链式调用示例。
     */
    fun loadText(baseUrl: String, path: String) = createClient(baseUrl)
        .execute(
            NetworkRequest(
                method = HttpMethod.GET,
                path = path,
                responseDecoder = ResponseDecoders.text()
            )
        )
        .filterIsInstance<NetworkEvent.Success<String>>()
        .map { event -> event.data }
}
