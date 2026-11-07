package com.creepersan.memorialday.module.network

/** 网络请求参数；host 为服务地址，url 为相对路径或完整 URL。 */
class NetworkRequest(
    val host: String,
    val url: String,
    val method: NetworkRequestMethod = NetworkRequestMethod.Get,
    val query: Map<String, Any>? = null,
    val data: NetworkRequestBody? = null,
    val headers: Map<String, String>? = null,
    // 整个请求的超时时间，单位为秒；为空时使用默认值。
    val timeout: Int? = null,
    // 业务请求默认需要登录；登录和刷新接口必须显式指定认证方式。
    val authentication: NetworkAuthentication = NetworkAuthentication.Required,
)
