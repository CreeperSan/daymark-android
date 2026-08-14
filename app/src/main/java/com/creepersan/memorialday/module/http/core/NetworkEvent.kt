package com.creepersan.memorialday.module.http.core

/**
 * 普通请求生命周期事件。
 */
sealed interface NetworkEvent<out T> {
    /**
     * 请求开始。
     */
    data class Started(val requestId: String) : NetworkEvent<Nothing>

    /**
     * 上传或下载进度。
     */
    data class Progress(
        val direction: Direction,
        val transferred: Long,
        val total: Long?
    ) : NetworkEvent<Nothing> {
        /**
         * 进度方向。
         */
        enum class Direction {
            /** 上传。 */
            UPLOAD,

            /** 下载。 */
            DOWNLOAD
        }
    }

    /**
     * 请求成功。
     */
    data class Success<T>(val value: T) : NetworkEvent<T>

    /**
     * 请求失败。
     */
    data class Failure(val error: NetworkError) : NetworkEvent<Nothing>

    /**
     * 请求取消。
     */
    data object Cancelled : NetworkEvent<Nothing>
}
