package com.creepersan.memorialday.module.kv

/** 可存入 KV 的对象，解码时返回对应的值。 */
interface KVSerializable<T> {

    /** 将当前值编码为字符串。 */
    fun encode(): String

    /** 将字符串解码为对应的值。 */
    fun decode(data: String): T

}
