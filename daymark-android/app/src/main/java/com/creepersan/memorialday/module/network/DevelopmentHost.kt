package com.creepersan.memorialday.module.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** 本机 IP 开发代理保留业务域名，供本地服务按 Host 分发路由。 */
internal object DevelopmentHost {
    private const val HOST_HEADER = "Host"

    /** 仅对本机名或 IP 代理设置 Host；自定义域名代理沿用其自己的 Host。 */
    fun headers(address: String, canonicalAddress: String): Map<String, String>? {
        val host = address.toHttpUrlOrNull()?.host ?: return null
        val canonicalHost = canonicalAddress.toHttpUrlOrNull()?.host ?: return null
        val isIp = host.contains(':') || (host.contains('.') && host.all { it.isDigit() || it == '.' })
        return if (host.equals("localhost", ignoreCase = true) || isIp) mapOf(HOST_HEADER to canonicalHost) else null
    }
}
