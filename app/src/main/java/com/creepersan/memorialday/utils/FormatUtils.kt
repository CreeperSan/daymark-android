package com.creepersan.memorialday.utils

import java.text.SimpleDateFormat
import java.util.Locale

object FormatUtils {
    private val timeFormatterYYYYMMDD by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    fun timestampToYYYYMMDD(timestamp: Long): String {
        return timeFormatterYYYYMMDD.format(timestamp)
    }

}