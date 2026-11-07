package com.creepersan.memorialday.module.kv

import android.content.Context
import com.creepersan.memorialday.application.MemorialDayApplication

/** 按模块读写 SharedPreferences。 */
object KV {

    /** 根据默认值的类型读取数据；序列化对象由默认值负责解码。 */
    fun <T> get(item: KVItem<T>): T {
        val preferences = MemorialDayApplication.instance.getSharedPreferences(item.module, Context.MODE_PRIVATE)
        val key = item.key()
        val defaultValue = item.defaultValue()

        // SharedPreferences 没有对应类型时，使用无损的基础类型存储。
        val value: Any = when (defaultValue) {
            is Boolean -> preferences.getBoolean(key, defaultValue)
            is Int -> preferences.getInt(key, defaultValue)
            is Long -> preferences.getLong(key, defaultValue)
            is Float -> preferences.getFloat(key, defaultValue)
            is String -> preferences.getString(key, defaultValue) ?: defaultValue
            is Byte -> preferences.getInt(key, defaultValue.toInt()).toByte()
            is Short -> preferences.getInt(key, defaultValue.toInt()).toShort()
            is Char -> preferences.getInt(key, defaultValue.code).toChar()
            is Double -> Double.fromBits(preferences.getLong(key, defaultValue.toBits()))
            is KVSerializable<*> -> {
                // 未保存时返回默认值，已保存时使用解码结果。
                preferences.getString(key, null)?.let { defaultValue.decode(it) } ?: defaultValue
            }
            else -> throw IllegalArgumentException("不支持的 SP 默认值类型: ${defaultValue?.let { it::class.java.name } ?: "null"}")
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    /** 按值的类型保存数据，写入后立即更新内存并异步持久化。 */
    fun <T> set(item: KVItem<T>, value: T) {
        val preferences = MemorialDayApplication.instance.getSharedPreferences(item.module, Context.MODE_PRIVATE)
        val key = item.key()
        val editor = preferences.edit()

        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Byte -> editor.putInt(key, value.toInt())
            is Short -> editor.putInt(key, value.toInt())
            is Char -> editor.putInt(key, value.code)
            is Double -> editor.putLong(key, value.toBits())
            is KVSerializable<*> -> editor.putString(key, value.encode())
            else -> throw IllegalArgumentException("不支持的 SP 值类型: ${value?.let { it::class.java.name } ?: "null"}")
        }

        editor.apply()
    }

    /** 删除指定配置，后续读取将返回默认值。 */
    fun remove(item: KVItem<*>) {
        val preferences = MemorialDayApplication.instance.getSharedPreferences(item.module, Context.MODE_PRIVATE)
        preferences.edit().remove(item.key()).apply()
    }


    /** 判断指定配置是否已保存，不受默认值影响。 */
    fun has(item: KVItem<*>): Boolean {
        val preferences = MemorialDayApplication.instance.getSharedPreferences(item.module, Context.MODE_PRIVATE)
        return preferences.contains(item.key())
    }

}
