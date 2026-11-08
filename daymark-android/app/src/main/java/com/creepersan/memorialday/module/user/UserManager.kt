package com.creepersan.memorialday.module.user

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.creepersan.memorialday.constance.KV_APPLICATION_DEV_MODE
import com.creepersan.memorialday.constance.KV_APPLICATION_PROXY_ACCOUNT
import com.creepersan.memorialday.constance.KV_USER_SESSION
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.module.network.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.rxjava3.core.Single
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** 本机缓存的账号及令牌，时间均为服务端返回的毫秒时间戳。 */
data class UserSession(
    val id: Long,
    val name: String,
    val avatar: String,
    val token: String,
    val tokenExpire: Long,
    val refreshToken: String,
    val refreshExpire: Long
)

/** 服务端明确拒绝刷新令牌。 */
private class AuthRejectedException(message: String) : IllegalStateException(message)

/** 负责账号会话持久化和令牌刷新；令牌使用 Android Keystore 加密并存入 KV。 */
object UserManager {
    private const val KEY_ALIAS = "daymark_session"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ACCOUNT_HOST = "https://account.creepersan.com"
    private const val LOGIN_PATH = "/api/login"
    private const val REFRESH_PATH = "/api/token/refresh"
    private const val REGISTER_CODE_PATH = "/api/register/request_auth_code"
    private const val REGISTER_PATH = "/api/register"
    private const val RESET_CODE_PATH = "/api/password/reset/request_auth_code"
    private const val RESET_PATH = "/api/password/reset"
    private const val REFRESH_AHEAD_MS = 60_000L
    private const val GCM_TAG_BITS = 128
    private val gson = Gson()
    @Volatile
    private var cached: UserSession? = null
    private var refreshInFlight: Single<UserSession>? = null
    private var loginInFlight: Single<UserSession>? = null
    private var sessionGeneration = 0L

    /** 读取已缓存会话；密钥失效或数据损坏时清除不可用数据。 */
    @Synchronized
    fun session(): UserSession? {
        cached?.let { return it }
        val encoded = KV.get(KV_USER_SESSION).ifEmpty { return null }
        return try {
            val parts = encoded.split(":")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
            )
            gson.fromJson(
                String(
                    cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    Charsets.UTF_8
                ), UserSession::class.java
            ).also { cached = it }
        } catch (_: Exception) {
            clear(); null
        }
    }

    /** 保存完整会话，避免用户与令牌分别写入造成不一致。 */
    @Synchronized
    private fun save(session: UserSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        ) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        KV.set(KV_USER_SESSION, value)
        cached = session
    }

    /** 清除本地登录状态。 */
    @Synchronized
    fun clear() {
        sessionGeneration++
        cached = null
        loginInFlight = null
        refreshInFlight = null
        KV.remove(KV_USER_SESSION)
    }

    /** 使用账号和密码登录；其他需要认证的请求可等待同一次登录结果。 */
    @Synchronized
    fun login(account: String, password: String): Single<UserSession> {
        loginInFlight?.let { return it }
        val generation = sessionGeneration
        lateinit var login: Single<UserSession>
        login = request(LOGIN_PATH, mapOf("account" to account, "password" to password), NetworkAuthentication.None)
            .map { data ->
                synchronized(this) {
                    check(sessionGeneration == generation) { "Session changed" }
                    parseSession(data, null).also(::save)
                }
            }
            // 分别处理成功和失败，避免 Single.doOnEvent 的可空参数触发 Kotlin 空值检查。
            .doOnSuccess { synchronized(this) { if (loginInFlight === login) loginInFlight = null } }
            .doOnError { synchronized(this) { if (loginInFlight === login) loginInFlight = null } }
            .cache()
        loginInFlight = login
        return login
    }

    /** 向邮箱发送注册或重置密码验证码。 */
    fun requestCode(email: String, register: Boolean): Single<JsonObject> =
        request(if (register) REGISTER_CODE_PATH else RESET_CODE_PATH, mapOf("email" to email), NetworkAuthentication.None)

    /** 使用邮箱验证码创建账号。 */
    fun register(account: String, email: String, password: String, code: String): Single<JsonObject> =
        request(REGISTER_PATH, mapOf("account" to account, "email" to email, "password" to password, "code" to code), NetworkAuthentication.None)

    /** 使用邮箱验证码重置密码。 */
    fun resetPassword(email: String, password: String, code: String): Single<JsonObject> =
        request(RESET_PATH, mapOf("email" to email, "new_password" to password, "code" to code), NetworkAuthentication.None)

    /** 获取可用令牌；同一时刻的刷新请求共享结果，网络错误不删除会话。 */
    @Synchronized
    fun validSession(): Single<UserSession> {
        loginInFlight?.let { return it }
        val current = session() ?: return Single.error(IllegalStateException("session_missing"))
        val now = System.currentTimeMillis()
        if (current.token.isNotBlank() && current.tokenExpire > now + REFRESH_AHEAD_MS) return Single.just(
            current
        )
        if (current.refreshExpire <= now || current.refreshToken.isBlank()) {
            clear(); return Single.error(IllegalStateException("session_expired"))
        }
        refreshInFlight?.let { return it }
        lateinit var refresh: Single<UserSession>
        refresh =
            request(REFRESH_PATH, mapOf("refresh_token" to current.refreshToken), NetworkAuthentication.Refresh(current.token))
                .map { data ->
                    synchronized(this) {
                        check(session() == current) { "Session changed" }
                        parseSession(data, current).also(::save)
                    }
                }
                .doOnError { if (it is AuthRejectedException && session() == current) clear() }
                // 刷新成功和失败都要释放共享请求，后续请求才能重新鉴权。
                .doOnSuccess { synchronized(this) { if (refreshInFlight === refresh) refreshInFlight = null } }
                .doOnError { synchronized(this) { if (refreshInFlight === refresh) refreshInFlight = null } }
                .cache()
        refreshInFlight = refresh
        return refresh
    }

    /** 请求账号接口；公共设备头和令牌由网络拦截器处理。 */
    private fun request(
        path: String,
        body: Map<String, String>,
        authentication: NetworkAuthentication
    ): Single<JsonObject> {
        val host = if (KV.get(KV_APPLICATION_DEV_MODE)) KV.get(KV_APPLICATION_PROXY_ACCOUNT)
            .ifBlank { ACCOUNT_HOST } else ACCOUNT_HOST
        return NetworkManager.request(
            NetworkRequest(
                host,
                path,
                NetworkRequestMethod.Post,
                data = NetworkRequestBody.Json(body),
                authentication = authentication
            )
        ).firstOrError().map { response ->
            if (response.responseCode !in 200..299) throw IllegalStateException("HTTP ${response.responseCode}")
            val json = JsonParser.parseString(response.bodyString()).asJsonObject
            val code = json.get("code")?.asInt
            if (code != 200) {
                val message = json.get("msg")?.asString ?: "Request failed"
                if (path == REFRESH_PATH && (code == 400 || code == 401)) throw AuthRejectedException(
                    message
                )
                throw IllegalStateException(message)
            }
            json.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        }
    }

    /** 校验服务端返回的账号和令牌，刷新时沿用本地用户资料。 */
    private fun parseSession(data: JsonObject, previous: UserSession?): UserSession {
        val user = data.getAsJsonObject("user")
        val token = data.getAsJsonObject("token") ?: error("Missing token")
        val result = UserSession(
            user?.get("id")?.asLong ?: previous?.id ?: 0,
            user?.get("nickname")?.asString ?: previous?.name.orEmpty(),
            user?.get("avatar")?.asString ?: previous?.avatar.orEmpty(),
            token.get("token")?.asString.orEmpty(),
            token.get("token_expire_time")?.asLong ?: 0,
            token.get("refresh_token")?.asString.orEmpty(),
            token.get("refresh_token_expire_time")?.asLong ?: 0
        )
        require(result.id > 0 && result.token.isNotBlank() && result.refreshToken.isNotBlank() && result.tokenExpire > 0 && result.refreshExpire > 0) { "Invalid session" }
        return result
    }

    /** 获取或创建仅属于本应用的加密密钥。 */
    private fun key(): javax.crypto.SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? javax.crypto.SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
            )
        }.generateKey()
    }
}
