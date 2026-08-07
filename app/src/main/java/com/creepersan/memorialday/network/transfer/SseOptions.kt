package com.creepersan.memorialday.network.transfer

/**
 * 服务端发送事件流的连接与重连配置。
 */
data class SseOptions(
    val maxReconnectAttempts: Int = 0,
    val initialLastEventId: String? = null
) {
    init {
        require(maxReconnectAttempts >= 0) { "最大重连次数不能小于 0" }
    }
}
