package com.creepersan.memorialday.module.network.host.account

import com.creepersan.memorialday.module.network.api.NetworkService
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountApi : NetworkService {

    @POST(LOGIN_PATH)
    suspend fun login(@Body request: LoginRequest): Response<AccountEnvelope<AccountLoginData>>

    @POST(CHECK_USER_EXIST_PATH)
    suspend fun checkUserExists(
        @Body request: CheckUserExistsRequest
    ): Response<AccountEnvelope<AccountAvailabilityData>>

    @POST(REQUEST_AUTH_CODE_PATH)
    suspend fun requestAuthCode(
        @Body request: RequestAuthCodeRequest
    ): Response<AccountEnvelope<AccountAuthCodeData>>

    @POST(REGISTER_PATH)
    suspend fun register(@Body request: RegisterRequest): Response<AccountEnvelope<AccountRegistrationData>>

    @POST(REFRESH_TOKEN_PATH)
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AccountEnvelope<AccountTokenData>>

    @POST(RENEW_TOKEN_PATH)
    suspend fun renewToken(): Response<AccountEnvelope<AccountTokenData>>

    companion object {
        const val LOGIN_PATH = "api/login"
        const val CHECK_USER_EXIST_PATH = "api/register/check_user_exist"
        const val REQUEST_AUTH_CODE_PATH = "api/register/request_auth_code"
        const val REGISTER_PATH = "api/register"
        const val REFRESH_TOKEN_PATH = "api/token/refresh"
        const val RENEW_TOKEN_PATH = "api/token/renew"
    }
}

data class LoginRequest(
    val account: String,
    val email: String,
    val password: String
)

data class CheckUserExistsRequest(
    val account: String,
    val email: String
)

data class RequestAuthCodeRequest(
    val email: String
)

data class RegisterRequest(
    val account: String,
    val password: String,
    val email: String,
    val code: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * The Go account service uses a code/message/data envelope. Alternate names retain compatibility
 * with earlier server responses while preserving the wire model at this boundary.
 */
data class AccountEnvelope<T>(
    @SerializedName(value = "code", alternate = ["status", "status_code"])
    val code: Int? = null,
    @SerializedName(value = "message", alternate = ["msg", "error", "detail"])
    val message: String? = null,
    @SerializedName(value = "data", alternate = ["result"])
    val data: T? = null
)

data class AccountLoginData(
    val user: AccountUserData? = null,
    val token: AccountTokenData? = null
)

data class AccountUserData(
    val id: Long? = null,
    val account: String? = null,
    val email: String? = null
)

data class AccountTokenData(
    @SerializedName(value = "token", alternate = ["access_token", "accessToken"])
    val accessToken: String? = null,
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"])
    val refreshToken: String? = null,
    @SerializedName(value = "token_expire_time", alternate = ["expires_at", "expiresAt"])
    val expiresAt: Long? = null,
    @SerializedName(value = "expires_in", alternate = ["expiresIn"])
    val expiresIn: Long? = null,
    @SerializedName(value = "account", alternate = ["username", "user_name"])
    val account: String? = null,
    val email: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId", "id"])
    val userId: Long? = null
)

data class AccountAvailabilityData(
    @SerializedName(value = "exists", alternate = ["user_exists", "userExists"])
    val exists: Boolean? = null,
    @SerializedName(value = "account_exist", alternate = ["account_exists", "accountExists"])
    val accountExists: Boolean? = null,
    @SerializedName(value = "email_exist", alternate = ["email_exists", "emailExists"])
    val emailExists: Boolean? = null
)

data class AccountAuthCodeData(
    @SerializedName("create_time")
    val createTime: Long? = null,
    @SerializedName("expire_time")
    val expireTime: Long? = null
)

data class AccountRegistrationData(
    val id: Long? = null,
    val account: String? = null,
    val email: String? = null,
    val role: Int? = null
)
