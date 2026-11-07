package com.creepersan.memorialday.utils.log

import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 将完整日志记录顺序写入按日期和大小分片的文件。 */
internal class LogFileWriter(private val privateDirectory: File) {

    companion object {
        private const val LOG_DIRECTORY = "logs"
        private const val MAX_FILE_SIZE = 500L * 1024
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    /** 同步选择分片并写入，避免多个线程同时写入时交错或覆盖。 */
    @Synchronized
    fun write(time: LocalDateTime, level: String, tag: String, message: String) {
        val directory = File(privateDirectory, LOG_DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("无法创建日志目录：$directory")
        }

        val date = time.format(DATE_FORMAT)
        // 转义换行符，使每条记录始终占用一行，并保留原始换行信息。
        val safeTag = escape(tag)
        val safeMessage = escape(message)
        val record = "${time.format(TIME_FORMAT)} [$level] [$safeTag] $safeMessage\n".toByteArray(Charsets.UTF_8)
        val prefix = "$date-"
        val sequence = directory.listFiles()?.mapNotNull { file ->
            if (file.isFile && file.name.startsWith(prefix) && file.name.endsWith(".log")) {
                file.name.removePrefix(prefix).removeSuffix(".log").toIntOrNull()?.takeIf { it > 0 }
            } else {
                null
            }
        }?.maxOrNull() ?: 1

        var file = File(directory, "$date-$sequence.log")
        // 单条记录超过限制时允许文件超出大小，不能把记录拆到两个文件。
        if (file.exists() && file.length() > 0 && file.length() + record.size > MAX_FILE_SIZE) {
            file = File(directory, "$date-${sequence + 1}.log")
        }
        file.appendBytes(record)
    }

    /** 转义反斜杠和换行符，便于区分原文与转义后的字符。 */
    private fun escape(value: String): String {
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
    }
}
