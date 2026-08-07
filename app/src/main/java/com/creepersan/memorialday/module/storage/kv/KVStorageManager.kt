package com.creepersan.memorialday.module.storage.kv

import android.content.Context
import java.util.EnumMap
import kotlin.enums.enumEntries

/**
 * KV 后端注册中心。
 *
 * 应用初始化时可以一次注册多个后端，具体使用哪个后端由 KVDefinition.type 决定。
 */
object KVStorageManager {

    private val operators = EnumMap<KVType, KVBaseOperator>(KVType::class.java)

    /**
     * 初始化 KV 后端。
     *
     * 应用启动时调用一次即可，所有内置后端会自动完成注册。
     *
     * @param context 应用上下文
     */
    @Synchronized
    fun initialize(context: Context) {
        operators.clear()
        // 使用类型到操作器的映射，保证多个后端可以同时存在且互不覆盖。
        enumEntries<KVType>().forEach { type ->
            operators[type] = when (type) {
                KVType.SharedPreferences -> KVSharedPreferencesOperator(context)
                KVType.DataStore -> KVDataStoreOperator(context)
            }
        }
    }

    /**
     * 获取指定类型的操作器。
     *
     * @param type 后端类型
     * @return 已注册的操作器
     */
    @Synchronized
    internal fun requireOperator(type: KVType): KVBaseOperator {
        return operators[type] ?: throw KVStorageNotInitializedException(
            "KV storage type '$type' has not been initialized"
        )
    }
}
