package com.creepersan.memorialday.common.definitions

import com.creepersan.memorialday.module.storage.kv.KVSerializable

/**
 * 应用模式：
 * 1、服务器模式
 * 2、本地模式
 */
enum class ApplicationMode(val mode: String) : KVSerializable<ApplicationMode> {
    NotDecide("not_decided"),
    Server("server"),
    Classic("locally"),

    ;

    override fun serialize(): String = mode

    override fun deserialize(storeValue: String) = entries.firstOrNull { it.mode == mode } ?: NotDecide

}