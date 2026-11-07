package com.creepersan.memorialday.constance

import com.creepersan.memorialday.module.kv.KVSerializable

/** 应用主题；存储标识固定，避免重命名枚举时影响已保存的数据。 */
enum class AppTheme(private val storageValue: String) : KVSerializable<AppTheme> {
    System("system"),
    Light("light"),
    Dark("dark");

    /** 将主题转换为固定的存储标识。 */
    override fun encode(): String = storageValue

    /** 根据存储标识还原主题，未知标识回退到当前默认值。 */
    override fun decode(data: String): AppTheme {
        return values().firstOrNull { it.storageValue == data } ?: this
    }
}
