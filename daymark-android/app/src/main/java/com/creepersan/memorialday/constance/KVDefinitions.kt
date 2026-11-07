package com.creepersan.memorialday.constance

import com.creepersan.memorialday.module.kv.KVItem

private const val kvModuleApplication = "application"

/*************************************************************************************************** 应用设置 */

// 主题设置
val KV_APPLICATION_THEME = KVItem(
    module = kvModuleApplication,
    key = { "theme" },
    defaultValue = { AppTheme.System },
)

// 模式
val KV_APPLICATION_MODE = KVItem(
    module = kvModuleApplication,
    key = { "mode" },
    defaultValue = { AppMode.Default },
)

// 新版语言和开发选项
val KV_APPLICATION_LANGUAGE = KVItem(kvModuleApplication, { "language" }, { "system" })
val KV_APPLICATION_DEV_MODE = KVItem(kvModuleApplication, { "developer_mode" }, { false })
val KV_APPLICATION_PROXY_ACCOUNT = KVItem(kvModuleApplication, { "proxy_account" }, { "" })
val KV_APPLICATION_PROXY_SERVER = KVItem(kvModuleApplication, { "proxy_server" }, { "" })

/*************************************************************************************************** 登录信息 */

// 加密后的完整用户会话；沿用原有文件及键，保留已登录用户的数据。
val KV_USER_SESSION = KVItem("user_session", { "session" }, { "" })
