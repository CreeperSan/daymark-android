package com.creepersan.memorialday.module.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl

/** 格式化调试网络日志，避免账号凭据和个人联系方式进入控制台。 */
internal object NetworkLogFormatter {
    private const val REDACTED = "[已隐藏]"
    private const val NON_JSON = "[非 JSON 内容已省略]"
    private val gson = Gson()
    private val sensitiveKeys = setOf("password", "passwd", "token", "authorization", "cookie", "secret", "apikey", "authcode", "verificationcode", "code", "email", "account", "phone", "devicecode")

    /** 地址只保留经过脱敏的查询参数。 */
    fun url(url: HttpUrl): String {
        val builder = url.newBuilder().username("").password("")
        url.queryParameterNames.forEach { name ->
            if (sensitive(name)) {
                val count = url.queryParameterValues(name).size
                builder.removeAllQueryParameters(name)
                repeat(count) { builder.addQueryParameter(name, REDACTED) }
            }
        }
        return builder.build().toString()
    }

    /** JSON 和表单递归遮盖敏感字段；二进制及未知格式不打印原文。 */
    fun body(body: NetworkRequestBody?): String = when (body) {
        null -> "(无)"
        is NetworkRequestBody.Json -> json(gson.toJson(body.value))
        is NetworkRequestBody.Form -> json(gson.toJson(body.values))
        is NetworkRequestBody.Text -> if (body.contentType.contains("json", ignoreCase = true)) json(body.value) else NON_JSON
        is NetworkRequestBody.Binary -> "[二进制内容，${body.value.size} 字节]"
    }

    /** 响应必须是 JSON 才输出，避免无法识别的文本泄露敏感内容。 */
    fun json(raw: String, preserveStatusCode: Boolean = false): String = try {
        val element = JsonParser.parseString(raw)
        if (!element.isJsonObject && !element.isJsonArray) NON_JSON else {
            redact(element, preserveStatusCode)
            gson.toJson(element)
        }
    } catch (_: Exception) { NON_JSON }

    private fun sensitive(key: String): Boolean {
        val normalized = key.lowercase().filter { it.isLetterOrDigit() }
        return sensitiveKeys.any { normalized == it || (it != "code" && normalized.contains(it)) }
    }

    private fun redact(value: JsonElement, isRoot: Boolean = false) {
        when {
            value.isJsonObject -> {
                val objectValue: JsonObject = value.asJsonObject
                objectValue.entrySet().toList().forEach { (key, child) ->
                    // 响应状态码是数值，不是登录验证码。
                    if (sensitive(key) && !(isRoot && key.equals("code", true) && child.isJsonPrimitive && child.asJsonPrimitive.isNumber)) objectValue.addProperty(key, REDACTED)
                    else redact(child)
                }
            }
            value.isJsonArray -> value.asJsonArray.forEach { redact(it) }
        }
    }
}
