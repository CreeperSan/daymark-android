package com.creepersan.memorialday.common.storage.kv

import com.creepersan.memorialday.module.storage.kv.KVDefinition
import com.creepersan.memorialday.module.storage.kv.KVType

private const val KVStorageApplication_Module = "user"

// 登录令牌
val KVStorageUser_Token = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "token",
    defaultValue = "",
)

// 登录令牌过期时间
val KVStorageUser_TokenExpireTime = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "token_expire_time",
    defaultValue = 0L,
)

// 刷新令牌
val KVStorageUser_RefreshToken = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "refresh_token",
    defaultValue = "",
)

// 刷新令牌过期时间
val KVStorageUser_RefreshTokenExpireTime = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "refresh_token_expire_time",
    defaultValue = 0L,
)

// 用户id
val KVStorageUser_UserID = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "user_id",
    defaultValue = 0L,
)

// 昵称
val KVStorageUser_NickName = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "nickname",
    defaultValue = "",
)

// 电子邮箱
val KVStorageUser_Email = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "email",
    defaultValue = "",
)

