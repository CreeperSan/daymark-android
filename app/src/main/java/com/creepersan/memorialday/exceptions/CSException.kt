package com.creepersan.memorialday.exceptions

open class CSException(msg: String = "发生异常") : Exception(msg) {

    override fun toString(): String {
        return "${javaClass.simpleName}(${message ?: "发生异常"})"
    }

}

fun Throwable.toCSException(message: String): CSException {
    if (this is CSException) {
        return this
    }
    return CSException(message)
}