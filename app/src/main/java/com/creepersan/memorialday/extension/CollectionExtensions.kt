package com.creepersan.memorialday.extension

fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) {
        this[index]
    } else {
        null
    }
}

fun <T> Array<T>.safeGet(index: Int): T? {
    return if (index in indices) {
        this[index]
    } else {
        null
    }
}

