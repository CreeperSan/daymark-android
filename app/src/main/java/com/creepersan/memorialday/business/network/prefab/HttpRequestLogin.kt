package com.creepersan.memorialday.business.network.prefab

import com.creepersan.memorialday.business.network.Network
import com.creepersan.memorialday.business.network.request
import com.creepersan.memorialday.module.http.request.HttpMethod
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.request.RequestBody
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

fun Network.login(
    account: String,
    email: String,
    password: String,
) : Flow<HttpResponse<NetworkResponseLogin>> {
    return NetworkRequest<HttpResponse<NetworkResponseLogin>>(
        method = HttpMethod.POST,
        host = Network.HostAccount,
        path = "/api/login",
        body = RequestBody.Json(
            value = mapOf(
                "account" to account,
                "email" to email,
                "password" to password,
            )
        ),
        decoder = { response ->
            Network.gson.fromJson(
                response.bodyText(),
                object : TypeToken<HttpResponse<NetworkResponseLogin>>() {}.type
            )
        }
    ).request()
}

data class NetworkResponseLogin(
    val token: Token,
    val user: User,
) {

    data class Token (
        val app: String,
        @SerializedName("device_code")
        val deviceCode: String,
        @SerializedName("device_name")
        val deviceName: String,
        @SerializedName("device_platform")
        val devicePlatform: String,
        val id: Long,
        @SerializedName("refresh_token")
        val refreshToken: String,
        @SerializedName("refresh_token_expire_time")
        val refreshTokenExpireTime: Long,
        val token: String,
        @SerializedName("token_expire_time")
        val tokenExpireTime: Long,
        @SerializedName("user_id")
        val userId: Long,
    )

    data class User (
        val account: String,
        val avatar: String,
        @SerializedName("create_time")
        val createTime: Long,
        val email: String,
        val id: Long,
        val nickname: String,
        val phone: String,
        val role: Int,
        val status: Int,
    )

}
