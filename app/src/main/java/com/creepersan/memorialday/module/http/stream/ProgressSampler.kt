package com.creepersan.memorialday.module.http.stream

import com.creepersan.memorialday.module.http.request.ProgressConfig

/**
 * 进度采样器。
 *
 * 同时满足最小时间间隔或最小字节增量之一时才发出事件，终态可通过 [flush] 补发最终值。
 */
class ProgressSampler(
    private val config: ProgressConfig,
    private val emit: (Long, Long?) -> Unit
) {
    private var lastBytes = 0L
    private var lastTime = 0L

    /**
     * 尝试发出一次进度。
     */
    fun report(bytes: Long, total: Long?) {
        val now = System.currentTimeMillis()
        val timeReady = now - lastTime >= config.minIntervalMillis
        val bytesReady = bytes - lastBytes >= config.minBytesDelta
        if (lastTime == 0L || timeReady || bytesReady) {
            lastBytes = bytes
            lastTime = now
            emit(bytes, total)
        }
    }

    /**
     * 强制补发最终进度。
     */
    fun flush(bytes: Long, total: Long?) {
        if (bytes != lastBytes || total == 0L) {
            lastBytes = bytes
            lastTime = System.currentTimeMillis()
            emit(bytes, total)
        }
    }
}
