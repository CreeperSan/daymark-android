package com.creepersan.memorialday.module.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime

/** 验证日志分片和记录完整性。 */
class LogFileWriterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun writesCompleteRecordsAcrossSizeAndDateBoundaries() {
        val directory = temporaryFolder.newFolder()
        val writer = LogFileWriter(directory)
        val time = LocalDateTime.of(2026, 11, 7, 23, 59)
        val largeMessage = "中".repeat(180_000)

        writer.write(time, "INFO", "标签", largeMessage)
        writer.write(time, "WARN", "标签", "第一行\n第二行")
        writer.write(time.plusDays(1), "ERROR", "标签", "次日")

        val logs = File(directory, "logs")
        val first = File(logs, "2026-11-07-1.log").readLines()
        val second = File(logs, "2026-11-07-2.log").readLines()
        assertEquals(1, first.size)
        assertTrue(first.single().endsWith(largeMessage))
        assertEquals(1, second.size)
        assertTrue(second.single().endsWith("第一行\\n第二行"))
        assertTrue(File(logs, "2026-11-08-1.log").readText().contains("次日"))

        // 重新创建写入器后仍应继续使用已有的最新分片。
        LogFileWriter(directory).write(time, "INFO", "标签", "重启后")
        assertEquals(2, File(logs, "2026-11-07-2.log").readLines().size)
    }
}
