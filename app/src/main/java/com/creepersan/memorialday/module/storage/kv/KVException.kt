package com.creepersan.memorialday.module.storage.kv

/** KV 存储异常基类。 */
open class KVException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** KV 定义参数不合法时抛出的异常。 */
class KVDefinitionException(message: String) : IllegalArgumentException(message)

/** KV 值类型不在支持范围内时抛出的异常。 */
class KVUnsupportedTypeException(type: Any?) : KVException(
    "Unsupported KV value type: ${type?.let { it::class.qualifiedName } ?: "null"}"
)

/** 存储内容无法转换为目标类型时抛出的异常。 */
class KVConversionException(
    message: String,
    cause: Throwable? = null,
) : KVException(message, cause)

/** 请求的 KV 后端尚未完成初始化时抛出的异常。 */
class KVStorageNotInitializedException(
    message: String = "KV storage has not been initialized. Call KVStorageManager.initialize() first."
) : KVException(message)
