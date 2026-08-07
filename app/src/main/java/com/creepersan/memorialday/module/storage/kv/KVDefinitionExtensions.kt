package com.creepersan.memorialday.module.storage.kv

/**
 * 读取当前 KV 定义对应的值。
 *
 * @return 存储值或定义中的默认值
 */
fun <T> KVDefinition<T>.get(): T {
    return KVStorageManager.requireOperator(type).get(this)
}

/**
 * 写入当前 KV 定义对应的值。
 *
 * @param value 待存储的值
 */
fun <T> KVDefinition<T>.set(value: T) {
    KVStorageManager.requireOperator(type).set(this, value)
}
