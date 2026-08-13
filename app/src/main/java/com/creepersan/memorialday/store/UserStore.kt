package com.creepersan.memorialday.store

import com.creepersan.memorialday.common.storage.kv.KVStorageUser_Email
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_NickName
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_RefreshToken
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_RefreshTokenExpireTime
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_Token
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_TokenExpireTime
import com.creepersan.memorialday.common.storage.kv.KVStorageUser_UserID
import com.creepersan.memorialday.module.storage.kv.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class UserStore : BaseStore() {

    /* 用户令牌 */
    private val _tokenStateFlow = MutableStateFlow<Token?>(null)
    val tokenStateFlow = _tokenStateFlow.asStateFlow()

    /* 用户令牌 */
    private val _userStateFlow = MutableStateFlow<UserInfo?>(null)
    val userStateFlow = _userStateFlow.asStateFlow()

    override suspend fun onInitialize() {
        _tokenStateFlow.update { null }
        _userStateFlow.update { null }
        // 读取缓存
        val cacheToken = KVStorageUser_Token.get()
        val cacheTokenExpireTime = KVStorageUser_TokenExpireTime.get()
        val cacheRefreshToken = KVStorageUser_RefreshToken.get()
        val cacheRefreshTokenExpireTime = KVStorageUser_RefreshTokenExpireTime.get()
        val cacheUserID = KVStorageUser_UserID.get()
        val cacheNickName = KVStorageUser_NickName.get()
        val cacheEmail = KVStorageUser_Email.get()
        // 检查数据合法

    }




    private fun actionLogin() {

    }


    private fun actionLogout() {

    }


    private fun actionRefreshToken() {

    }

    /**
     * 登录令牌结构化信息
     */
    data class Token (
        val token: String,
        val tokenExpireTime: Long,
        val refreshToken: String,
        val refreshTokenExpireTime: Long,
    )

    /**
     * 用户信息结构化信息
     */
    data class UserInfo (
        val userID: Long,
        val nickname: String,
        val email: String,
    )

}