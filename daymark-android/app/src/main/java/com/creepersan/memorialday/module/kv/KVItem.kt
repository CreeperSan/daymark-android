package com.creepersan.memorialday.module.kv

data class KVItem<T> (
    val module: String,
    val key: () -> String,
    val defaultValue: () -> T,
)

fun <T> KVItem<T>.get(): T  {
    return KV.get(this)
}

fun <T> KVItem<T>.set(value: T) {
    return KV.set(this, value)
}
