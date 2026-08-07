package com.creepersan.memorialday.module.storage.kv

/**
 * KV 存储后端类型。
 */
enum class KVType {
    /** Android SharedPreferences 后端。 */
    SharedPreferences,

    /** Android Preferences DataStore 后端。 */
    DataStore,
}