package com.creepersan.memorialday.extension

import android.content.res.TypedArray
import androidx.core.content.res.getBooleanOrThrow
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getStringOrThrow


fun TypedArray.requireBooleanIfDefine(styleRes: Int, action: (value: Boolean) -> Unit) {
    if (hasValue(styleRes)) {
        action.invoke(getBooleanOrThrow(styleRes))
    }
}

fun TypedArray.requireColorIfDefine(styleRes: Int, action: (value: Int) -> Unit) {
    if (hasValue(styleRes)) {
        action.invoke(getColorOrThrow(styleRes))
    }
}

fun TypedArray.requireDimensionIfDefine(styleRes: Int, action: (value: Float) -> Unit) {
    if (hasValue(styleRes)) {
        action.invoke(getDimensionOrThrow(styleRes))
    }
}


fun TypedArray.requireTextIfDefine(styleRes: Int, action: (value: String) -> Unit) {
    if (hasValue(styleRes)) {
        action.invoke(getStringOrThrow(styleRes))
    }
}
