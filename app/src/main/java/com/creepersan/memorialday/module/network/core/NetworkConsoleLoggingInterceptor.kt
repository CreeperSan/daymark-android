package com.creepersan.memorialday.module.network.core

import okio.Buffer
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale

/**
 * Transport middleware that writes sanitized request and response diagnostics to [NetworkLogger].
 *
 * It never consumes response bodies and omits one-shot, duplex, binary, and oversized request
 * bodies so logging cannot change request behavior or expose large payloads.
 */
class NetworkConsoleLoggingInterceptor(
    private val logger: NetworkLogger,
    private val maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.log(requestMessage(request))
        return try {
            chain.proceed(request).also { response ->
                logger.log(responseMessage(request, response))
            }
        } catch (error: IOException) {
            logger.log(
                buildString {
                    append("HTTP FAILED\n")
                    append("method=").append(request.method).append('\n')
                    append("url=").append(sanitizeUrl(request.url)).append('\n')
                    append("error=").append(error.javaClass.simpleName)
                    append(": ").append(error.message.orEmpty())
                }
            )
            throw error
        }
    }

    private fun requestMessage(request: Request): String = buildString {
        append("HTTP REQUEST\n")
        append("method=").append(request.method).append('\n')
        append("url=").append(sanitizeUrl(request.url)).append('\n')
        append("headers=").append(sanitizeHeaders(request.headers.toMultimap())).append('\n')
        append("params=").append(requestBody(request))
    }

    private fun responseMessage(request: Request, response: Response): String = buildString {
        append("HTTP RESPONSE\n")
        append("method=").append(request.method).append('\n')
        append("url=").append(sanitizeUrl(request.url)).append('\n')
        append("code=").append(response.code).append('\n')
        append("headers=").append(sanitizeHeaders(response.headers.toMultimap())).append('\n')
        append("result=").append(responseBody(response))
    }

    private fun sanitizeUrl(url: HttpUrl): String {
        if (url.querySize == 0) {
            return url.toString()
        }
        return url.newBuilder().apply {
            for (index in 0 until url.querySize) {
                val name = url.queryParameterName(index)
                if (name.isSensitive()) {
                    setQueryParameter(name, REDACTED_VALUE)
                }
            }
        }.build().toString()
    }

    private fun sanitizeHeaders(headers: Map<String, List<String>>): Map<String, List<String>> =
        headers.mapValues { (name, values) ->
            if (name.isSensitive()) {
                List(values.size) { REDACTED_VALUE }
            } else {
                values
            }
        }

    private fun requestBody(request: Request): String {
        val body = request.body ?: return "<empty>"
        if (body.isDuplex() || body.isOneShot()) {
            return "<omitted: streaming body>"
        }
        val contentLength = body.contentLength()
        if (contentLength < 0 || contentLength > maxBodyBytes) {
            return "<omitted: ${if (contentLength < 0) "unknown" else contentLength} bytes>"
        }
        val contentType = body.contentType()?.toString().orEmpty()
        if (!contentType.isTextual()) {
            return "<omitted: $contentType>"
        }
        return Buffer().use { buffer ->
            body.writeTo(buffer)
            sanitizePayload(buffer.readUtf8())
        }
    }

    private fun responseBody(response: Response): String {
        val body = response.body ?: return "<empty>"
        val contentType = body.contentType()?.toString().orEmpty()
        if (!contentType.isTextual()) {
            return "<omitted: $contentType>"
        }
        return sanitizePayload(response.peekBody(maxBodyBytes).string())
    }

    private fun sanitizePayload(value: String): String {
        return JSON_SENSITIVE_VALUE.replace(value) { match ->
            "${match.groupValues[1]}$REDACTED_VALUE${match.groupValues[3]}"
        }.let { jsonRedacted ->
            FORM_SENSITIVE_VALUE.replace(jsonRedacted) { match ->
                val delimiter = match.value.take(1).takeIf { it == "&" || it.first().isWhitespace() }.orEmpty()
                val name = match.value.removePrefix(delimiter).substringBefore('=')
                "$delimiter$name=$REDACTED_VALUE"
            }
        }
    }

    private fun String.isSensitive(): Boolean = lowercase(Locale.US) in SENSITIVE_KEYS

    private fun String.isTextual(): Boolean {
        val normalized = lowercase(Locale.US)
        return normalized.startsWith("text/") ||
            normalized.contains("json") ||
            normalized.contains("xml") ||
            normalized.contains("x-www-form-urlencoded")
    }

    private companion object {
        const val DEFAULT_MAX_BODY_BYTES = 64L * 1024L
        const val REDACTED_VALUE = "***"
        val SENSITIVE_KEYS = setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "cs-token",
            "token",
            "access_token",
            "refresh_token",
            "password"
        )
        val JSON_SENSITIVE_VALUE = Regex(
            """(?i)("(?:(?:password|token|access_token|refresh_token|authorization))"\s*:\s*")([^"]*)(")"""
        )
        val FORM_SENSITIVE_VALUE = Regex(
            """(?i)(^|[&\s])((?:password|token|access_token|refresh_token|authorization))=[^&\s]*"""
        )
    }
}
