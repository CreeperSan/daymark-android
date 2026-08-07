package com.creepersan.memorialday.module.storage.kv

/**
 * 描述一个具有明确类型的 KV 项。
 *
 * @param T KV 值类型
 * @property module 存储模块，用于隔离不同业务的数据
 * @property type 当前定义使用的存储后端类型
 */
class KVDefinition<T>(
    /** 存储模块名称。 */
    val module: String,
    private val key: String? = null,
    private val keyDynamic: (() -> String?)? = null,
    private val defaultValue: T? = null,
    private val defaultValueDynamic: (() -> T?)? = null,
    /** 当前定义使用的存储后端。 */
    val type: KVType = KVType.SharedPreferences,
) {

    init {
        // 定义创建时校验静态配置，避免错误配置在实际读写时才暴露。
        if (module.isBlank()) {
            throw KVDefinitionException("KV module must not be blank")
        }
        if ((key == null) == (keyDynamic == null)) {
            throw KVDefinitionException("Exactly one of key and keyDynamic must be provided")
        }
        if ((defaultValue == null) == (defaultValueDynamic == null)) {
            throw KVDefinitionException(
                "Exactly one of defaultValue and defaultValueDynamic must be provided"
            )
        }
    }

    /**
     * 解析当前操作使用的键。
     *
     * @return 非空且非空白的存储键
     */
    internal fun resolveKey(): String {
        val resolvedKey = key ?: keyDynamic?.invoke()
        if (resolvedKey.isNullOrBlank()) {
            throw KVDefinitionException("KV key must not be blank")
        }
        return resolvedKey
    }

    /**
     * 解析当前操作使用的默认值或反序列化模板。
     *
     * @return 非空默认值
     */
    internal fun resolveDefault(): T {
        return (defaultValue ?: defaultValueDynamic?.invoke())
            ?: throw KVDefinitionException("KV default value must not be null")
    }

}