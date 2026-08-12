package com.creepersan.memorialday.common.definitions

import com.creepersan.memorialday.module.storage.kv.KVSerializable

/**
 * 应用模式：
 * 1、服务器模式
 * 2、本地模式
 */
enum class AppMode(val mode: String) : KVSerializable<AppMode> {
    NotDecide("not_decided"),
    Server("server"),
    Classic("locally"),

    ;

    override fun serialize(): String = mode

    override fun deserialize(storeValue: String) = entries.firstOrNull { it.mode == mode } ?: NotDecide

}