package com.creepersan.memorialday.module

object CompareUtils {

    fun compareCalendar(
        year1: Int = 0,
        month1: Int = 0,
        day1: Int = 0,
        year2: Int = 0,
        month2: Int = 0,
        day2: Int = 0,
    ): CompareResult {
        val num1 = day1 + month1 * 100L + year1 * 10000L
        val num2 = day2 + month2 * 100L + year2 * 10000L
        return when {
            num1 > num2 -> CompareResult.Greater
            num1 < num2 -> CompareResult.Less
            else -> CompareResult.Equal
        }
    }

    fun compareChineseCalendar(
        year1: Int = 0,
        month1: Int = 0,
        day1: Int = 0,
        isLeapMonth1: Boolean = false,
        year2: Int = 0,
        month2: Int = 0,
        day2: Int = 0,
        isLeapMonth2: Boolean = false,
    ): CompareResult {
        val num1 = day1 + month1 * 100L + (if (isLeapMonth1) 10000L else 0L) + year1 * 100000L
        val num2 = day2 + month2 * 100L + (if (isLeapMonth2) 10000L else 0L) + year2 * 100000L
        return when {
            num1 > num2 -> CompareResult.Greater
            num1 < num2 -> CompareResult.Less
            else -> CompareResult.Equal
        }
    }




}

enum class CompareResult {
    Equal,
    Less,
    Greater,
}

