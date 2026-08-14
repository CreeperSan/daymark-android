package com.creepersan.memorialday.module.http.request

/**
 * 类型安全的请求属性键。
 *
 * 属性仅用于中间件和请求编排元数据，不应用来承载大文件或敏感原文。
 */
class AttributeKey<T>(val name: String)

/**
 * 请求级不可变自定义属性。
 */
class RequestAttributes private constructor(
    private val values: Map<AttributeKey<*>, Any?>
) {
    /**
     * 读取指定类型的属性。
     */
    operator fun <T> get(key: AttributeKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values[key] as? T
    }

    /**
     * 返回增加属性后的新对象。
     */
    fun <T> with(key: AttributeKey<T>, value: T): RequestAttributes =
        RequestAttributes(values + (key to value))

    /**
     * 返回当前属性快照。
     */
    fun snapshot(): RequestAttributes = RequestAttributes(values.toMap())

    companion object {
        /**
         * 创建空属性集合。
         */
        fun empty(): RequestAttributes = RequestAttributes(emptyMap())
    }
}
