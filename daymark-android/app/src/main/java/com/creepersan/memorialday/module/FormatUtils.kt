package com.creepersan.memorialday.module

import java.text.SimpleDateFormat
import java.util.Locale

object FormatUtils {
    private val timeFormatterYYYYMMDD by lazy { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    fun timestampToYYYYMMDD(timestamp: Long): String {
        return timeFormatterYYYYMMDD.format(timestamp)
    }

}