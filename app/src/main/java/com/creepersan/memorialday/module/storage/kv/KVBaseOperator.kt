package com.creepersan.memorialday.module.storage.kv

/**
 * KV 存储后端的统一操作接口。
 *
 * 不同存储实现只负责数据读写，定义解析和后端选择由上层统一处理。
 */
interface KVBaseOperator {

    /**
     * 根据定义读取值。
     *
     * @param definition KV 定义
     * @return 存储值；键不存在时返回定义中的默认值
     */
    fun <T> get(definition: KVDefinition<T>): T

    /**
     * 根据定义写入值。
     *
     * @param definition KV 定义
     * @param value 待存储的值
     */
    fun <T> set(definition: KVDefinition<T>, value: T)

}