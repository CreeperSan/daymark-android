package com.creepersan.memorialday.network.transfer

import com.google.gson.Gson
import com.google.protobuf.Parser
import okhttp3.Response
import java.io.File
import java.lang.reflect.Type

/**
 * 将 HTTP 响应转换为业务数据的可扩展接口。
 *
 * 解码过程运行在 IO 调度器，下载解码器可以经由 [ProgressReporter] 报告进度。
 */
fun interface ResponseDecoder<T> {
    suspend fun decode(response: Response, reporter: ProgressReporter): T
}

/** 下载和上传包装器使用的进度回调。 */
fun interface ProgressReporter {
    fun report(transferredBytes: Long, totalBytes: Long?)
}

/**
 * 框架内置的常用响应解码器。
 */
object ResponseDecoders {
    fun text(): ResponseDecoder<String> = ResponseDecoder { response, _ ->
        response.body?.string() ?: ""
    }

    fun bytes(): ResponseDecoder<ByteArray> = ResponseDecoder { response, _ ->
        response.body?.bytes() ?: ByteArray(0)
    }

    fun <T> json(gson: Gson, type: Type): ResponseDecoder<T> = ResponseDecoder { response, _ ->
        val body = response.body?.charStream() ?: error("响应体为空")
        body.use { gson.fromJson<T>(it, type) }
    }

    fun <T : Any> protobuf(parser: Parser<T>): ResponseDecoder<T> = ResponseDecoder { response, _ ->
        val body = response.body ?: error("响应体为空")
        body.byteStream().use(parser::parseFrom)
    }

    /**
     * 将响应安全写入同目录临时文件，完整写入后再原子替换目标文件。
     */
    fun file(target: File, resume: Boolean = false): ResponseDecoder<File> = ResponseDecoder { response, reporter ->
        val body = response.body ?: error("响应体为空")
        val temporary = File(target.parentFile, "${target.name}.download")
        val append = resume && response.code == HTTP_PARTIAL_CONTENT
        if (append) {
            val expectedRange = "bytes ${temporary.length()}-"
            require(response.header("Content-Range")?.startsWith(expectedRange) == true) {
                "服务端返回的 Content-Range 与本地临时文件不匹配"
            }
        }
        DownloadWriter.write(
            body = body,
            target = target,
            append = append,
            reporter = reporter
        )
    }

    private const val HTTP_PARTIAL_CONTENT = 206
}
