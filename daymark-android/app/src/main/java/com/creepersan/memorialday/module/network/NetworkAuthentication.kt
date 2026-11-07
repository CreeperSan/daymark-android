package com.creepersan.memorialday.module.network

/** 请求的鉴权方式；刷新请求携带旧令牌，但不等待自身完成。 */
sealed class NetworkAuthentication {
    object Required : NetworkAuthentication()
    object None : NetworkAuthentication()
    class Refresh(val token: String) : NetworkAuthentication()
}
