package com.creepersan.memorialday.utils.log

import com.creepersan.memorialday.application.MemorialDayApplication
import java.time.LocalDateTime

/** 应用日志入口，将日志按日期和文件大小保存到应用私有目录。 */
object Log {

    private const val ERROR_TAG = "FileLog"
    private val writer by lazy { LogFileWriter(MemorialDayApplication.instance.noBackupFilesDir) }

    /** 记录普通信息。 */
    fun info(message: String, tag: String) {
        write("INFO", message, tag)
    }

    /** 记录警告信息。 */
    fun warning(message: String, tag: String) {
        write("WARN", message, tag)
    }

    /** 记录错误信息。 */
    fun error(message: String, tag: String) {
        write("ERROR", message, tag)
    }

    /** 写入失败时输出到 Logcat，避免日志故障影响业务。 */
    private fun write(level: String, message: String, tag: String) {
        try {
            writer.write(LocalDateTime.now(), level, tag, message)
        } catch (exception: Exception) {
            android.util.Log.e(ERROR_TAG, "写入日志失败", exception)
        }
    }
}
