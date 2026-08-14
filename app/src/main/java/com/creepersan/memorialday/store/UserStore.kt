package com.creepersan.memorialday.store

import android.util.Log
import com.creepersan.memorialday.business.network.Network
import com.creepersan.memorialday.business.network.prefab.NetworkResponseLogin
import com.creepersan.memorialday.business.network.prefab.login
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_Account
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_Email
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_NickName
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_RefreshToken
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_RefreshTokenExpireTime
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_Token
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_TokenExpireTime
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_UserID
import com.creepersan.memorialday.exceptions.CSException
import com.creepersan.memorialday.exceptions.toCSException
import com.creepersan.memorialday.module.log.Logger
import com.creepersan.memorialday.module.storage.kv.get
import com.creepersan.memorialday.module.storage.kv.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update


class UserStore : BaseStore() {

    /* 登录装套 */
    private val _loginStateStateFlow = MutableStateFlow<LoginState>(LoginState.NotLogin)
    val loginStateStateFlow = _loginStateStateFlow.asStateFlow()
    val loginState get() = loginStateStateFlow.value

    /* 用户信息 */
    private val _userInfoStateFlow = MutableStateFlow<UserInfo?>(null)
    val userInfoStateFlow = _userInfoStateFlow.asStateFlow()
    val user get() = userInfoStateFlow.value



    override suspend fun onInitialize() {
        _loginStateStateFlow.update { LoginState.NotLogin }
        _userInfoStateFlow.update { null }
        // 读取缓存
        val cacheToken = KVStorageUser_Token.get()
        val cacheTokenExpireTime = KVStorageUser_TokenExpireTime.get()
        val cacheRefreshToken = KVStorageUser_RefreshToken.get()
        val cacheRefreshTokenExpireTime = KVStorageUser_RefreshTokenExpireTime.get()
        val cacheUserID = KVStorageUser_UserID.get()
        val cacheUserAccount = KVStorageUser_Account.get()
        val cacheNickName = KVStorageUser_NickName.get()
        val cacheEmail = KVStorageUser_Email.get()
        // 更新信息
        val currentTimestamp = System.currentTimeMillis()
        val isTokenValid = cacheRefreshToken.isNotBlank() && cacheRefreshTokenExpireTime > (currentTimestamp + 5 * 60 * 1000) // 5分钟容差
        if (isTokenValid) {
            _userInfoStateFlow.update {
                UserInfo(
                    userID = cacheUserID,
                    account = cacheUserAccount,
                    nickname = cacheNickName,
                    email = cacheEmail,
                )
            }
            _loginStateStateFlow.update {
                LoginState.LoginSuccess (
                    token = cacheToken,
                    tokenExpireTime = cacheTokenExpireTime,
                    refreshToken = cacheRefreshToken,
                    refreshTokenExpireTime = cacheRefreshTokenExpireTime,
                )
            }
        }


    }


    /**
     * 登录操作
     */
    fun actionLogin(
        account: String = "",
        email: String = "",
        password: String = "",
    ) : Flow<NetworkResponseLogin> {
        return flow {
            // 参数校验
            if (account.isBlank() && email.isBlank()) {
                throw CSException("请输入帐号")
            }
            if (password.isBlank()) {
                throw CSException("请输入密码")
            }
            // 更新状态
            _loginStateStateFlow.update { LoginState.Logging }
            // 进行网络请求
            emitAll(Network.login(
                account = account,
                email = email,
                password = password,
            ))
        }.map { response ->
            if (!response.success()) {
                throw CSException(response.msg)
            }
            return@map response.data ?: throw CSException("登录失败，服务器返回数据为空")
        }.map { response ->
            // 读取参数
            val responseToken = response.token.token
            val responseTokenExpireTime = response.token.tokenExpireTime
            val responseRefreshToken = response.token.refreshToken
            val responseRefreshTokenExpireTime = response.token.refreshTokenExpireTime
            val responseUserID = response.user.id
            val responseUserNickname = response.user.nickname
            val responseUserAccount = response.user.account
            val responseUserEmail = response.user.email
            // 更新存储
            KVStorageUser_Token.set(responseToken)
            KVStorageUser_TokenExpireTime.set(responseTokenExpireTime)
            KVStorageUser_RefreshToken.set(responseRefreshToken)
            KVStorageUser_RefreshTokenExpireTime.set(responseRefreshTokenExpireTime)
            KVStorageUser_UserID.set(responseUserID)
            KVStorageUser_Account.set(responseUserAccount)
            KVStorageUser_NickName.set(responseUserNickname)
            KVStorageUser_Email.set(responseUserEmail)
            // 更新状态
            _loginStateStateFlow.update {
                LoginState.LoginSuccess(
                    token = responseToken,
                    tokenExpireTime = responseTokenExpireTime,
                    refreshToken = responseRefreshToken,
                    refreshTokenExpireTime = responseRefreshTokenExpireTime,
                )
            }
            _userInfoStateFlow.update { old ->
                UserInfo(
                    userID = responseUserID,
                    account = responseUserAccount,
                    nickname = responseUserNickname,
                    email = responseUserEmail,
                )
            }
            // 返回数据
            return@map response
        }.catch { err ->
            Logger.error(tag, "登录失败，err=$err")
            throw err.toCSException("登录失败，请稍后重试")
        }
    }


    fun actionLogout() {

    }


    fun actionRefreshToken(): Flow<TokenRefreshState> {
        return flowOf(TokenRefreshState.Fail)
    }


    /**
     * 用户登录状态
     */
    sealed interface LoginState {

        object NotLogin : LoginState

        object Logging : LoginState

        data class LoginSuccess(
            val token: String,
            val tokenExpireTime: Long,
            val refreshToken: String,
            val refreshTokenExpireTime: Long,
        ) : LoginState

    }

    /**
     * 用户信息结构化信息
     */
    data class UserInfo (
        val userID: Long,
        val account: String,
        val nickname: String,
        val email: String,
    )


    /**
     * token 刷新结果
     */
    sealed interface TokenRefreshState {

        object Fail : TokenRefreshState

        object TokenInvalid : TokenRefreshState

        data class Success(val token: NetworkResponseLogin.Token) : TokenRefreshState

    }


}