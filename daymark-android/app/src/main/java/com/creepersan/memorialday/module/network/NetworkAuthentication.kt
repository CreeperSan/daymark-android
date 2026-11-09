package com.creepersan.memorialday.module.network

/** 请求的鉴权方式。已指定令牌的请求不再查找当前会话。 */
sealed class NetworkAuthentication {
    object Required : NetworkAuthentication()
    object None : NetworkAuthentication()
    class Token(val token: String) : NetworkAuthentication()
}
