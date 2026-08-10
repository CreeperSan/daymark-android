package com.creepersan.memorialday.module.storage.kv

/**
 * 为复杂 KV 类型提供字符串序列化和反序列化能力。
 *
 * @param T 反序列化后的类型
 */
interface KVSerializable<T> {

    /**
     * 将当前对象转换为存储字符串。
     *
     * @return 可持久化的字符串
     */
    fun serialize(): String

    /**
     * 根据存储内容恢复对象。
     *
     * @param storeValue 存储中的原始值
     * @return 恢复后的对象
     */
    fun deserialize(storeValue: String): T

}