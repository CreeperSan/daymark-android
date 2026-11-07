package com.creepersan.memorialday.module.network

/** 常用 HTTP 请求方法。 */
enum class NetworkRequestMethod(val value: String) {
    Get("GET"),
    Post("POST"),
    Put("PUT"),
    Patch("PATCH"),
    Delete("DELETE"),
}
