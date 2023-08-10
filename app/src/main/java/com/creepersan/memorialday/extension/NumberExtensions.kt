package com.creepersan.memorialday.extension

import android.content.res.Resources
import android.util.TypedValue


val Number.dp: Float get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)

val Number.dpi: Int get() = this.dp.toInt()

val Number.sp: Float get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics)

val Number.spi: Int get() = this.sp.toInt()

