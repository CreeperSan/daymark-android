package com.creepersan.memorialday.module.network.transfer

import kotlinx.coroutines.ensureActive
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.coroutineContext

/**
 * 为上传请求体增加节流的进度报告。
 */
internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: ProgressReporter,
    private val minimumStepBytes: Long = DEFAULT_PROGRESS_STEP_BYTES
) : RequestBody() {
    override fun contentType() = delegate.contentType()

    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = object : ForwardingSink(sink) {
            private var transferred = 0L
            private var lastReported = 0L

            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                transferred += byteCount
                if (transferred - lastReported >= minimumStepBytes) {
                    onProgress.report(transferred, contentLength().takeIf { it >= 0L })
                    lastReported = transferred
                }
            }
        }.buffer()
        delegate.writeTo(countingSink)
        countingSink.flush()
        onProgress.report(contentLength().coerceAtLeast(0L), contentLength().takeIf { it >= 0L })
    }

    private companion object {
        const val DEFAULT_PROGRESS_STEP_BYTES = 32L * 1024L
    }
}

/**
 * 将下载内容写入临时文件，并在成功时提交为目标文件。
 */
internal object DownloadWriter {
    suspend fun write(
        body: okhttp3.ResponseBody,
        target: File,
        append: Boolean,
        reporter: ProgressReporter
    ): File {
        val temporary = File(target.parentFile, "${target.name}.download")
        target.parentFile?.mkdirs()
        if (!append && temporary.exists() && !temporary.delete()) {
            throw IOException("无法清理旧的临时下载文件")
        }

        val initialBytes = if (append) temporary.length() else 0L
        val total = body.contentLength().takeIf { it >= 0L }?.plus(initialBytes)
        var transferred = initialBytes
        try {
            body.byteStream().use { input ->
                java.io.FileOutputStream(temporary, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) {
                            break
                        }
                        output.write(buffer, 0, count)
                        transferred += count
                        reporter.report(transferred, total)
                    }
                    output.fd.sync()
                }
            }
            moveAtomically(temporary, target)
            reporter.report(transferred, total)
            return target
        } catch (error: Throwable) {
            if (!append && temporary.exists() && !temporary.delete()) {
                // 保留原始异常，清理失败不应掩盖下载失败。
            }
            throw error
        }
    }

    private fun moveAtomically(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
