package com.creepersan.memorialday.network.transfer

import com.google.protobuf.MessageLite
import java.io.File

/**
 * 请求载荷的通用描述，避免业务代码直接依赖 OkHttp 的可变构建 API。
 */
sealed interface RequestPayload {
    data object Empty : RequestPayload

    data class Json(
        val value: Any,
        val mediaType: String = JSON_MEDIA_TYPE
    ) : RequestPayload

    data class Form(val fields: Map<String, String>) : RequestPayload

    data class Multipart(val parts: List<MultipartPart>) : RequestPayload

    data class Protobuf(val message: MessageLite) : RequestPayload

    data class Binary(
        val bytes: ByteArray,
        val mediaType: String = RequestPayload.OCTET_STREAM_MEDIA_TYPE
    ) : RequestPayload

    companion object {
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream"
    }
}

/**
 * Multipart 的字段或文件分段。
 */
sealed interface MultipartPart {
    data class Field(val name: String, val value: String) : MultipartPart

    data class FilePart(
        val name: String,
        val file: File,
        val fileName: String = file.name,
        val mediaType: String = RequestPayload.OCTET_STREAM_MEDIA_TYPE
    ) : MultipartPart

    data class BytesPart(
        val name: String,
        val bytes: ByteArray,
        val fileName: String,
        val mediaType: String = RequestPayload.OCTET_STREAM_MEDIA_TYPE
    ) : MultipartPart
}
