package com.creepersan.memorialday.module.storage.kv

import android.content.Context
import android.content.SharedPreferences

/**
 * 基于 SharedPreferences 的同步 KV 操作器。
 *
 * 原生支持的基础类型直接使用对应 API，其余基础类型统一按字符串存储。
 *
 * @param context 用于创建存储文件的上下文
 */
class KVSharedPreferencesOperator(context: Context) : KVBaseOperator {

    private val applicationContext = context.applicationContext

    /**
     * 读取定义对应的值。
     *
     * @param definition KV 定义
     * @return 存储值或默认值
     */
    override fun <T> get(definition: KVDefinition<T>): T {
        val key = definition.resolveKey()
        val defaultValue = definition.resolveDefault()
        val preferences = preferences(definition)
        if (!preferences.contains(key)) {
            // 键不存在时直接返回默认值，但仍需校验默认值类型。
            ensureSupported(defaultValue)
            return defaultValue
        }

        return try {
            // 读取类型以默认值为准，保证泛型擦除后仍能确定目标类型。
            @Suppress("UNCHECKED_CAST")
            when (defaultValue) {
                is String -> preferences.getString(key, null)
                    ?: throw KVConversionException("Stored String value is null for key '$key'")
                is Boolean -> preferences.getBoolean(key, defaultValue)
                is Char -> preferences.requireString(key).singleOrThrow(key)
                is Byte -> preferences.requireString(key).toByteStrict(key)
                is Short -> preferences.requireString(key).toShortStrict(key)
                is Int -> preferences.getInt(key, defaultValue)
                is Long -> preferences.getLong(key, defaultValue)
                is Float -> preferences.getFloat(key, defaultValue)
                is Double -> preferences.requireString(key).toDoubleStrict(key)
                is KVSerializable<*> -> deserialize(defaultValue, preferences.requireString(key))
                else -> throw KVUnsupportedTypeException(defaultValue)
            } as T
        } catch (exception: KVException) {
            throw exception
        } catch (exception: ClassCastException) {
            throw KVConversionException("Stored value has an incompatible type for key '$key'", exception)
        } catch (exception: NumberFormatException) {
            throw KVConversionException("Stored value cannot be converted for key '$key'", exception)
        } catch (exception: RuntimeException) {
            throw KVConversionException("Failed to read KV value for key '$key'", exception)
        }
    }

    /**
     * 写入定义对应的值。
     *
     * @param definition KV 定义
     * @param value 待存储的值
     */
    override fun <T> set(definition: KVDefinition<T>, value: T) {
        val key = definition.resolveKey()
        ensureSupported(value)
        val editor = preferences(definition).edit()
        // SharedPreferences 只原生支持部分类型，其余类型使用字符串保持可逆转换。
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Char -> editor.putString(key, value.toString())
            is Byte -> editor.putString(key, value.toString())
            is Short -> editor.putString(key, value.toString())
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Double -> editor.putString(key, value.toString())
            is KVSerializable<*> -> editor.putString(key, value.serialize())
            else -> throw KVUnsupportedTypeException(value)
        }
        editor.apply()
    }

    /** 获取模块对应的 SharedPreferences 文件。 */
    private fun <T> preferences(definition: KVDefinition<T>): SharedPreferences {
        return applicationContext.getSharedPreferences(
            "kv_${definition.module}",
            Context.MODE_PRIVATE
        )
    }

    /** 校验值是否属于当前版本支持的类型。 */
    private fun ensureSupported(value: Any?) {
        when (value) {
            is String,
            is Boolean,
            is Char,
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double,
            is KVSerializable<*> -> Unit
            else -> throw KVUnsupportedTypeException(value)
        }
    }

    /** 使用默认值实例提供的反序列化逻辑恢复复杂对象。 */
    @Suppress("UNCHECKED_CAST")
    private fun <T> deserialize(deserializer: KVSerializable<*>, value: String): T {
        return (deserializer as KVSerializable<T>).deserialize(value)
    }

    /** 读取必须为字符串的存储内容。 */
    private fun SharedPreferences.requireString(key: String): String {
        return getString(key, null)
            ?: throw KVConversionException("Stored value is not a String for key '$key'")
    }

    /** 将字符串严格转换为单个字符。 */
    private fun String.singleOrThrow(key: String): Char {
        if (length != 1) {
            throw KVConversionException("Stored value is not a single character for key '$key'")
        }
        return single()
    }

    /** 将字符串严格转换为 Byte。 */
    private fun String.toByteStrict(key: String): Byte {
        return toByteOrNull()
            ?: throw KVConversionException("Stored value is not a Byte for key '$key'")
    }

    /** 将字符串严格转换为 Short。 */
    private fun String.toShortStrict(key: String): Short {
        return toShortOrNull()
            ?: throw KVConversionException("Stored value is not a Short for key '$key'")
    }

    /** 将字符串严格转换为 Double。 */
    private fun String.toDoubleStrict(key: String): Double {
        return toDoubleOrNull()
            ?: throw KVConversionException("Stored value is not a Double for key '$key'")
    }

}