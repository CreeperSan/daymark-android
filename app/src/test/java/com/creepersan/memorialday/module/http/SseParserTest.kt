package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.sse.SseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SSE 协议解析测试。
 */
class SseParserTest {
    /**
     * 验证多行 data、事件名称和事件标识能够正确合并。
     */
    @Test
    fun parseMultiLineEvent() {
        val parser = SseParser()

        val events = parser.feed(
            "event: update\nid: 7\ndata: first\ndata: second\n\n"
        )

        assertEquals(1, events.size)
        assertEquals("update", events.single().event)
        assertEquals("7", events.single().id)
        assertEquals("first\nsecond", events.single().data)
    }

    /**
     * 验证注释心跳不会产生业务事件。
     */
    @Test
    fun ignoreHeartbeatComment() {
        val parser = SseParser()

        val events = parser.feed(": heartbeat\n\n")

        assertEquals(0, events.size)
        assertNull(parser.finish().firstOrNull())
    }

    /**
     * 验证多字节 UTF-8 字符跨字节块时不会被截断。
     */
    @Test
    fun decodeUtf8AcrossChunks() {
        val parser = SseParser()
        val bytes = "data: 你好\n\n".encodeToByteArray()

        parser.feed(bytes.copyOfRange(0, 8))
        val events = parser.feed(bytes.copyOfRange(8, bytes.size))

        assertEquals("你好", events.single().data)
    }
}
