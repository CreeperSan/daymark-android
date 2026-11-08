package com.creepersan.memorialday.module.anniversary

/** 农历日期的中文显示名称；其他语言沿用数字，避免硬编码中文。 */
object LunarDateLabels {
    private val months = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    private val digits = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    fun month(value: Int): String = months.getOrNull(value - 1) ?: value.toString()

    fun day(value: Int): String = when (value) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        in 1..9 -> "初${digits[value]}"
        in 11..19 -> "十${digits[value - 10]}"
        in 21..29 -> "廿${digits[value - 20]}"
        else -> value.toString()
    }
}
