package com.creepersan.memorialday.module.storage.kv

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * 基于 Preferences DataStore 的 KV 操作器。
 *
 * 为兼容现有同步 get/set API，内部使用 IO 协程等待 DataStore 完成读写。
 *
 * @param context 用于创建 DataStore 文件的上下文
 */
class KVDataStoreOperator(context: Context) : KVBaseOperator {

    private val applicationContext = context.applicationContext
    private val stores = mutableMapOf<String, DataStore<Preferences>>()
    private val storeLock = Any()

    /**
     * 读取定义对应的值。
     *
     * @param definition KV 定义
     * @return 存储值或默认值
     */
    override fun <T> get(definition: KVDefinition<T>): T {
        val key = definition.resolveKey()
        val defaultValue = definition.resolveDefault()
        val preferences = readPreferences(definition)
        if (!preferences.asMap().keys.any { it.name == key }) {
            // 通过键名检查是否存在，避免不同 Preferences.Key<T> 泛型造成误判。
            ensureSupported(defaultValue)
            return defaultValue
        }

        return try {
            // DataStore 的键类型由默认值决定，确保读取结果与定义类型一致。
            @Suppress("UNCHECKED_CAST")
            when (defaultValue) {
                is String -> preferences[stringPreferencesKey(key)]
                    ?: throw KVConversionException("Stored String value is null for key '$key'")
                is Boolean -> preferences[booleanPreferencesKey(key)]
                    ?: throw KVConversionException("Stored value is not a Boolean for key '$key'")
                is Char -> preferences[stringPreferencesKey(key)]
                    ?.singleOrThrow(key)
                    ?: throw KVConversionException("Stored value is not a Char for key '$key'")
                is Byte -> preferences[stringPreferencesKey(key)]
                    ?.toByteStrict(key)
                    ?: throw KVConversionException("Stored value is not a Byte for key '$key'")
                is Short -> preferences[stringPreferencesKey(key)]
                    ?.toShortStrict(key)
                    ?: throw KVConversionException("Stored value is not a Short for key '$key'")
                is Int -> preferences[intPreferencesKey(key)]
                    ?: throw KVConversionException("Stored value is not an Int for key '$key'")
                is Long -> preferences[longPreferencesKey(key)]
                    ?: throw KVConversionException("Stored value is not a Long for key '$key'")
                is Float -> preferences[floatPreferencesKey(key)]
                    ?: throw KVConversionException("Stored value is not a Float for key '$key'")
                is Double -> preferences[stringPreferencesKey(key)]
                    ?.toDoubleStrict(key)
                    ?: throw KVConversionException("Stored value is not a Double for key '$key'")
                is KVSerializable<*> -> preferences[stringPreferencesKey(key)]
                    ?.let { deserialize(defaultValue, it) }
                    ?: throw KVConversionException("Stored serializable value is null for key '$key'")
                else -> throw KVUnsupportedTypeException(defaultValue)
            } as T
        } catch (exception: KVException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw KVConversionException("Failed to read DataStore value for key '$key'", exception)
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
        // 当前公开 API 是同步方法，因此在 IO 调度器中等待 DataStore 写入完成。
        runBlocking(Dispatchers.IO) {
            store(definition).edit { preferences ->
                when (value) {
                    is String -> preferences[stringPreferencesKey(key)] = value
                    is Boolean -> preferences[booleanPreferencesKey(key)] = value
                    is Char -> preferences[stringPreferencesKey(key)] = value.toString()
                    is Byte -> preferences[stringPreferencesKey(key)] = value.toString()
                    is Short -> preferences[stringPreferencesKey(key)] = value.toString()
                    is Int -> preferences[intPreferencesKey(key)] = value
                    is Long -> preferences[longPreferencesKey(key)] = value
                    is Float -> preferences[floatPreferencesKey(key)] = value
                    is Double -> preferences[stringPreferencesKey(key)] = value.toString()
                    is KVSerializable<*> -> preferences[stringPreferencesKey(key)] = value.serialize()
                    else -> throw KVUnsupportedTypeException(value)
                }
            }
        }
    }

    /** 同步读取 DataStore 当前快照。 */
    private fun <T> readPreferences(definition: KVDefinition<T>): Preferences {
        return runBlocking(Dispatchers.IO) {
            store(definition).data.first()
        }
    }

    /**
     * 获取模块对应的 DataStore 实例。
     *
     * 同一模块必须复用同一个实例，否则 DataStore 会认为同一文件存在多个活跃数据源。
     */
    private fun <T> store(definition: KVDefinition<T>): DataStore<Preferences> {
        synchronized(storeLock) {
            return stores.getOrPut(definition.module) {
                PreferenceDataStoreFactory.create {
                    applicationContext.preferencesDataStoreFile("kv_${definition.module}")
                }
            }
        }
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