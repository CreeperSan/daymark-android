package com.creepersan.memorialday.constance

import com.creepersan.memorialday.utils.kv.KVItem

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
