package com.creepersan.memorialday.utils.chinese_calendar.model

/**
 * 农历信息
 * @property id
 * @property year 公历年份
 * @property month 公历月份
 * @property day 公历天
 * @property weekday 星期
 * @property lunarYear 农历年份
 * @property lunarMonth 农历月份
 * @property lunarDay 农历天
 * @property isLeapMonth 是否闰月
 * @property lunarYearName 农历年份名称
 * @property solarTerm 节气
 * @property zodiac 生肖
 */
data class ChineseCalendar(
    val id: Int,
    val year: Int,
    val month: Int,
    val day: Int,
    val weekday: Int,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean,
    val lunarYearName: String,
    val solarTerm: ChineseCalendarSolarTerm,
    val zodiac: ChineseCalendarZodiac,
) {

    override fun equals(other: Any?): Boolean {
        return other is ChineseCalendar && other.id == id
    }

    override fun toString(): String {
        return "ChineseCalendar($year-$month-$day ($weekday), $lunarYear-$lunarMonth-$lunarDay ($isLeapMonth), $lunarYearName, $solarTerm, $zodiac)"
    }

}


fun ChineseCalendar.lunarMonthToString(): String {
    var resultStr = when (month) {
        1 -> "正月"
        2 -> "二月"
        3 -> "三月"
        4 -> "四月"
        5 -> "五月"
        6 -> "六月"
        7 -> "七月"
        8 -> "八月"
        9 -> "九月"
        10 -> "十月"
        11 -> "冬月"
        12 -> "腊月"
        else -> ""
    }
    if (isLeapMonth) {
        resultStr = "润$resultStr"
    }
    return resultStr
}


fun ChineseCalendar.lunarDayToString(): String {
    // 特殊处理
    if (lunarDay == 10) {
        return "初十"
    } else if (lunarDay == 20) {
        return "二十"
    }

    val lunarDay1 = lunarDay % 10
    val lunarDay10 = lunarDay / 10
    var resultStr = ""
    resultStr += when (lunarDay10) {
        0 -> "初"
        1 -> "十"
        2 -> "廿"
        3 -> "三"
        else -> ""
    }
    resultStr += when(lunarDay1) {
        0 -> "十"
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "七"
        8 -> "八"
        9 -> "九"
        else -> ""
    }
    return resultStr
}


fun ChineseCalendar.getFullLunarDayString(): String {
    var resultStr = ""
    resultStr += lunarYearName
    resultStr += "年 "
    if (isLeapMonth) {
        resultStr += "闰"
    }
    resultStr += lunarMonthToString()
    resultStr += lunarDayToString()
    return resultStr
}

fun ChineseCalendar.getFullGregorianDayString(): String {
    return "${year}年${month}月${day}日"
}

