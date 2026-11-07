package com.creepersan.memorialday.constance

import com.creepersan.memorialday.module.kv.KVSerializable

/** 应用运行模式；存储标识固定，避免重命名枚举时影响已保存的数据。 */
enum class AppMode(private val storageValue: String) : KVSerializable<AppMode> {
    Classic("classic"),
    Default("default");

    /** 将模式转换为固定的存储标识。 */
    override fun encode(): String = storageValue

    /** 根据存储标识还原模式，未知标识回退到当前默认值。 */
    override fun decode(data: String): AppMode {
        return values().firstOrNull { it.storageValue == data } ?: this
    }
}
