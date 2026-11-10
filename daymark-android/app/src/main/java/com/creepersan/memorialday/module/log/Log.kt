package com.creepersan.memorialday.module.log

import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.BuildConfig
import java.time.LocalDateTime
import java.util.concurrent.Executors

/** 应用日志入口，将日志按日期和文件大小保存到应用私有目录。 */
object Log {

    private const val ERROR_TAG = "FileLog"
    private val writer by lazy { LogFileWriter(MemorialDayApplication.instance.noBackupFilesDir) }
    private val executor = Executors.newSingleThreadExecutor()

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
        val time = LocalDateTime.now()
        if (BuildConfig.DEBUG) try {
            when (level) {
                "ERROR" -> android.util.Log.e(tag, message)
                "WARN" -> android.util.Log.w(tag, message)
                else -> android.util.Log.i(tag, message)
            }
        } catch (_: RuntimeException) { /* 本地 JVM 测试没有 Android Log 实现。 */ }
        // 文件写入串行执行，避免界面操作因磁盘 IO 卡顿。
        executor.execute {
            try { writer.write(time, level, tag, message) }
            catch (exception: Exception) {
                try { android.util.Log.e(ERROR_TAG, "写入日志失败", exception) } catch (_: RuntimeException) { }
            }
        }
    }
}
