package com.creepersan.memorialday.module.http.sse

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * 增量 SSE 帧解析器。
 *
 * 解析器允许一次输入半行或多行字节，只有遇到空行才提交完整事件。
 */
class SseParser {
    private val lineBuffer = StringBuilder()
    private val dataLines = mutableListOf<String>()
    private var eventName: String? = null
    private var eventId: String? = null
    private var retryMillis: Long? = null
    private var pendingBytes = ByteArray(0)
    private var heartbeatPending = false

    /**
     * 输入一段 UTF-8 文本并返回已完成事件。
     */
    fun feed(text: String): List<RawSseEvent> {
        val events = mutableListOf<RawSseEvent>()
        text.forEach { character ->
            if (character == '\n') {
                processLine(lineBuffer.toString().removeSuffix("\r"), events)
                lineBuffer.clear()
            } else {
                lineBuffer.append(character)
            }
        }
        return events
    }

    /**
     * 输入字节块并进行可跨块的 UTF-8 解码。
     */
    fun feed(bytes: ByteArray): List<RawSseEvent> {
        val combined = pendingBytes + bytes
        val input = ByteBuffer.wrap(combined)
        val output = CharBuffer.allocate(combined.size)
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(input, output, false)
        pendingBytes = ByteArray(input.remaining()).also { input.get(it) }
        output.flip()
        return feed(output.toString())
    }

    /**
     * 读取并清除最近一次收到的注释心跳标记。
     */
    fun consumeHeartbeat(): Boolean {
        val result = heartbeatPending
        heartbeatPending = false
        return result
    }

    /**
     * 在连接结束时提交最后一帧。
     */
    fun finish(): List<RawSseEvent> {
        require(pendingBytes.isEmpty()) { "SSE 响应在 UTF-8 字符中间结束" }
        val events = mutableListOf<RawSseEvent>()
        if (lineBuffer.isNotEmpty()) {
            processLine(lineBuffer.toString().removeSuffix("\r"), events)
            lineBuffer.clear()
        }
        commitEvent()?.let(events::add)
        return events
    }

    private fun processLine(line: String, events: MutableList<RawSseEvent>) {
        if (line.isEmpty()) {
            commitEvent()?.let(events::add)
            return
        }
        if (line.startsWith(":")) {
            heartbeatPending = true
            return
        }
        val separator = line.indexOf(':')
        val field = if (separator >= 0) line.substring(0, separator) else line
        val value = if (separator >= 0) {
            line.substring(separator + 1).removePrefix(" ")
        } else {
            ""
        }
        when (field) {
            "event" -> eventName = value
            "id" -> eventId = value
            "retry" -> retryMillis = value.toLongOrNull()
            "data" -> dataLines += value
        }
    }

    private fun commitEvent(): RawSseEvent? {
        if (dataLines.isEmpty()) {
            eventName = null
            retryMillis = null
            return null
        }
        val event = RawSseEvent(
            event = eventName,
            id = eventId,
            retryMillis = retryMillis,
            data = dataLines.joinToString("\n")
        )
        dataLines.clear()
        eventName = null
        retryMillis = null
        return event
    }
}
