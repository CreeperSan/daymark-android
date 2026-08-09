package com.creepersan.memorialday.module.network.host.account

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

sealed interface AccountResult<out T> {
    data class Success<T>(val data: T) : AccountResult<T>
    data class Failure(val error: AccountError) : AccountResult<Nothing>
}

sealed interface AccountError {
    val message: String

    data class Validation(override val message: String) : AccountError
    data class DuplicateSubmission(override val message: String = "请求正在处理中") : AccountError
    data class Parameter(override val message: String) : AccountError
    data class Authentication(override val message: String) : AccountError
    data class Server(override val message: String) : AccountError
    data class Network(override val message: String) : AccountError
    data class MalformedResponse(override val message: String = "服务器响应格式错误") : AccountError
    data class Cancelled(override val message: String = "请求已取消") : AccountError
}

class AccountRepository(
    private val serviceFactory: AccountApiProvider,
    private val tokenStorage: AccountTokenStorage,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) {
    private val submissions = ConcurrentHashMap<Operation, AtomicBoolean>().apply {
        Operation.entries.forEach { operation -> put(operation, AtomicBoolean(false)) }
    }

    suspend fun login(accountOrEmail: String, password: String): AccountResult<AccountSession> {
        validateRequired(accountOrEmail, "账号或邮箱")?.let { return AccountResult.Failure(it) }
        validateRequired(password, "密码")?.let { return AccountResult.Failure(it) }
        return submit(Operation.LOGIN) {
            val account = accountOrEmail.trim().takeUnless { EMAIL_REGEX.matches(it) }.orEmpty()
            val email = accountOrEmail.trim().takeIf { EMAIL_REGEX.matches(it) }.orEmpty()
            decodeLogin(serviceFactory.create().login(LoginRequest(account, email, password)))
        }
    }

    suspend fun checkUserExists(account: String, email: String): AccountResult<AccountAvailabilityData> {
        validateAccount(account)?.let { return AccountResult.Failure(it) }
        validateEmail(email)?.let { return AccountResult.Failure(it) }
        return submit(Operation.CHECK_EXISTENCE) {
            decode(serviceFactory.create().checkUserExists(CheckUserExistsRequest(account.trim(), email.trim())))
        }
    }

    suspend fun requestAuthCode(email: String): AccountResult<AccountAuthCodeData> {
        validateEmail(email)?.let { return AccountResult.Failure(it) }
        return submit(Operation.REQUEST_CODE) {
            decode(serviceFactory.create().requestAuthCode(RequestAuthCodeRequest(email.trim())))
        }
    }

    suspend fun register(
        account: String,
        password: String,
        email: String,
        code: String
    ): AccountResult<AccountRegistrationData> {
        validateAccount(account)?.let { return AccountResult.Failure(it) }
        validateRequired(password, "密码")?.let { return AccountResult.Failure(it) }
        validateEmail(email)?.let { return AccountResult.Failure(it) }
        validateRequired(code, "验证码")?.let { return AccountResult.Failure(it) }
        return submit(Operation.REGISTER) {
            decode(
                serviceFactory.create().register(
                    RegisterRequest(account.trim(), password, email.trim(), code.trim())
                )
            )
        }
    }

    suspend fun refreshToken(): AccountResult<AccountSession> {
        val refreshToken = tokenStorage.refreshToken()
            ?: return AccountResult.Failure(AccountError.Authentication("登录已失效，请重新登录"))
        return submit(Operation.REFRESH) {
            decodeToken(serviceFactory.createForRefresh().refreshToken(RefreshTokenRequest(refreshToken)))
        }
    }

    suspend fun renewToken(): AccountResult<AccountSession> {
        if (tokenStorage.current() == null) {
            return AccountResult.Failure(AccountError.Authentication("登录已失效，请重新登录"))
        }
        return submit(Operation.RENEW) {
            decodeToken(serviceFactory.create().renewToken())
        }
    }

    private suspend fun <T> submit(
        operation: Operation,
        request: suspend () -> AccountResult<T>
    ): AccountResult<T> {
        val pending = submissions.getValue(operation)
        if (!pending.compareAndSet(false, true)) {
            return AccountResult.Failure(AccountError.DuplicateSubmission())
        }
        return try {
            request()
        } catch (_: CancellationException) {
            AccountResult.Failure(AccountError.Cancelled())
        } catch (_: JsonParseException) {
            AccountResult.Failure(AccountError.MalformedResponse())
        } catch (_: EOFException) {
            AccountResult.Failure(AccountError.MalformedResponse())
        } catch (_: IOException) {
            AccountResult.Failure(AccountError.Network("网络连接失败，请检查网络后重试"))
        } catch (_: IllegalStateException) {
            AccountResult.Failure(AccountError.MalformedResponse())
        } finally {
            pending.set(false)
        }
    }

    private fun decodeLogin(response: Response<AccountEnvelope<AccountLoginData>>): AccountResult<AccountSession> {
        return when (val decoded = decode(response)) {
            is AccountResult.Failure -> decoded
            is AccountResult.Success -> {
                val login = decoded.data
                sessionFromToken(login.token, login.user)
            }
        }
    }

    private fun decodeToken(response: Response<AccountEnvelope<AccountTokenData>>): AccountResult<AccountSession> {
        return when (val decoded = decode(response)) {
            is AccountResult.Failure -> decoded
            is AccountResult.Success -> sessionFromToken(decoded.data, null)
        }
    }

    private fun sessionFromToken(
        token: AccountTokenData?,
        user: AccountUserData?
    ): AccountResult<AccountSession> {
        val accessToken = token?.accessToken?.trim()
        if (accessToken.isNullOrEmpty()) {
            return AccountResult.Failure(AccountError.MalformedResponse("服务器未返回有效登录凭据"))
        }
        val existing = tokenStorage.current()
        val session = AccountSession(
            accessToken = accessToken,
            refreshToken = token.refreshToken?.trim()?.ifBlank { null } ?: existing?.refreshToken,
            account = user?.account?.trim()?.ifBlank { null } ?: existing?.account,
            email = user?.email?.trim()?.ifBlank { null } ?: existing?.email,
            userId = user?.id ?: existing?.userId,
            expiresAtEpochMillis = resolveExpiry(token)
        )
        tokenStorage.update(session)
        return AccountResult.Success(session)
    }

    private fun <T> decode(
        response: Response<AccountEnvelope<T>>,
        allowEmptyData: Boolean = false
    ): AccountResult<T> {
        if (!response.isSuccessful) {
            return AccountResult.Failure(errorFromHttp(response.code(), response.message()))
        }
        val envelope = response.body()
            ?: return AccountResult.Failure(AccountError.MalformedResponse())
        if (!isSuccess(envelope.code)) {
            return AccountResult.Failure(errorFromCode(envelope.code, envelope.message))
        }
        val data = envelope.data
        if (data == null && !allowEmptyData) {
            return AccountResult.Failure(AccountError.MalformedResponse("服务器响应缺少数据"))
        }
        @Suppress("UNCHECKED_CAST")
        return AccountResult.Success(data as T)
    }

    private fun isSuccess(code: Int?): Boolean = code == 0 || code in 200..299

    private fun errorFromHttp(code: Int, message: String): AccountError {
        return when (code) {
            400, 404, 409, 422 -> AccountError.Parameter(message.ifBlank { "请求参数错误" })
            401, 403 -> AccountError.Authentication(message.ifBlank { "登录已失效，请重新登录" })
            in 500..599 -> AccountError.Server(message.ifBlank { "服务器暂时不可用" })
            else -> AccountError.Network(message.ifBlank { "请求失败" })
        }
    }

    private fun errorFromCode(code: Int?, message: String?): AccountError {
        val resolvedMessage = message?.takeIf { it.isNotBlank() } ?: "请求失败"
        return when (code) {
            400, 404, 409, 422, 1001, 1002 -> AccountError.Parameter(resolvedMessage)
            401, 403, 1003, 1004 -> AccountError.Authentication(resolvedMessage)
            null -> AccountError.MalformedResponse()
            else -> AccountError.Server(resolvedMessage)
        }
    }

    private fun resolveExpiry(token: AccountTokenData): Long {
        token.expiresAt?.let { timestamp ->
            return if (timestamp in 1..9_999_999_999L) timestamp * MILLIS_PER_SECOND else timestamp
        }
        token.expiresIn?.let { duration ->
            return nowEpochMillis() + duration * MILLIS_PER_SECOND
        }
        return tokenStorage.current()?.expiresAtEpochMillis ?: 0L
    }

    private fun validateAccount(value: String): AccountError.Validation? {
        return validateRequired(value, "账号") ?: if (value.trim().length > MAX_ACCOUNT_LENGTH) {
            AccountError.Validation("账号长度不能超过${MAX_ACCOUNT_LENGTH}个字符")
        } else {
            null
        }
    }

    private fun validateEmail(value: String): AccountError.Validation? {
        return validateRequired(value, "邮箱") ?: if (!EMAIL_REGEX.matches(value.trim())) {
            AccountError.Validation("请输入有效的邮箱地址")
        } else {
            null
        }
    }

    private fun validateRequired(value: String, field: String): AccountError.Validation? {
        return when {
            value.isBlank() -> AccountError.Validation("${field}不能为空")
            value.length > MAX_FIELD_LENGTH -> {
                AccountError.Validation("${field}长度不能超过${MAX_FIELD_LENGTH}个字符")
            }
            else -> null
        }
    }

    private enum class Operation {
        LOGIN,
        CHECK_EXISTENCE,
        REQUEST_CODE,
        REGISTER,
        REFRESH,
        RENEW
    }

    private companion object {
        const val MAX_ACCOUNT_LENGTH = 64
        const val MAX_FIELD_LENGTH = 128
        const val MILLIS_PER_SECOND = 1_000L
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
