package com.creepersan.memorialday.network.transfer

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 负责将公开的 [RequestPayload] 编码为 OkHttp 请求体。
 *
 * 业务可通过实现该接口支持额外媒体类型，框架按注册顺序选择首个可处理的编解码器。
 */
interface PayloadCodec {
    fun supports(payload: RequestPayload): Boolean

    fun encode(payload: RequestPayload): RequestBody
}

/** 默认请求载荷编解码器集合。 */
internal class DefaultPayloadCodec(
    private val gson: Gson
) : PayloadCodec {
    override fun supports(payload: RequestPayload): Boolean = payload !is RequestPayload.Empty

    override fun encode(payload: RequestPayload): RequestBody {
        return when (payload) {
            RequestPayload.Empty -> error("空载荷不应请求编码")
            is RequestPayload.Json -> gson.toJson(payload.value)
                .toRequestBody(payload.mediaType.toMediaType())

            is RequestPayload.Form -> FormBodyFactory.create(payload.fields)
            is RequestPayload.Multipart -> createMultipart(payload.parts)
            is RequestPayload.Protobuf -> payload.message.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaType())

            is RequestPayload.Binary -> payload.bytes.toRequestBody(payload.mediaType.toMediaType())
        }
    }

    private fun createMultipart(parts: List<MultipartPart>): RequestBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                parts.forEach { part ->
                    when (part) {
                        is MultipartPart.Field -> addFormDataPart(part.name, part.value)
                        is MultipartPart.FilePart -> addFormDataPart(
                            part.name,
                            part.fileName,
                            part.file.asRequestBody(part.mediaType.toMediaType())
                        )

                        is MultipartPart.BytesPart -> addFormDataPart(
                            part.name,
                            part.fileName,
                            part.bytes.toRequestBody(part.mediaType.toMediaType())
                        )
                    }
                }
            }
            .build()
    }
}

private object FormBodyFactory {
    fun create(fields: Map<String, String>): RequestBody {
        return okhttp3.FormBody.Builder()
            .apply { fields.forEach(::add) }
            .build()
    }
}
