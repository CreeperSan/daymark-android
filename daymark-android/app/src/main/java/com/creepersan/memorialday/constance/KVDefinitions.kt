package com.creepersan.memorialday.constance

import com.creepersan.memorialday.module.kv.KVItem

private const val kvModuleApplication = "application"

// 修改欢迎页内容并需要再次展示时，递增当前版本；旧引导对应的版本保持不变。
const val WELCOME_SCREEN_CURRENT_VERSION = 1
const val WELCOME_SCREEN_LEGACY_VERSION = 1

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

// 用户已完成或跳过的欢迎页版本；零表示尚未完成。
val KV_WELCOME_SCREEN_SHOW_VERSION = KVItem(kvModuleApplication, { "welcomeScreenShowVersion" }, { 0 })

// 新版语言和开发选项
val KV_APPLICATION_LANGUAGE = KVItem(kvModuleApplication, { "language" }, { "system" })
val KV_APPLICATION_DEV_MODE = KVItem(kvModuleApplication, { "developer_mode" }, { false })
val KV_APPLICATION_PROXY_ACCOUNT = KVItem(kvModuleApplication, { "proxy_account" }, { "" })
val KV_APPLICATION_PROXY_SERVER = KVItem(kvModuleApplication, { "proxy_server" }, { "" })

/*************************************************************************************************** 登录信息 */

// 登录成功后按用户选择保存的账号。
val KV_REMEMBERED_ACCOUNT = KVItem(kvModuleApplication, { "remembered_account" }, { "" })

// 加密后的完整用户会话；沿用原有文件及键，保留已登录用户的数据。
val KV_USER_SESSION = KVItem("user_session", { "session" }, { "" })
