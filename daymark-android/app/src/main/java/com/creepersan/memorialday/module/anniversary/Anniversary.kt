package com.creepersan.memorialday.module.anniversary

import com.google.gson.annotations.SerializedName

/** 服务端纪念日的原始日期和下一次发生时间。 */
data class Anniversary(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    @SerializedName("calendar_type") val calendarType: String = CALENDAR_SOLAR,
    @SerializedName("loop_type") val loopType: String = LOOP_NONE,
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    @SerializedName("is_leap_month") val isLeapMonth: Int = 0,
    @SerializedName("next_timestamp") val nextTimestamp: Long = 0,
) {
    companion object {
        const val CALENDAR_SOLAR = "solar"
        const val CALENDAR_LUNAR = "lunar"
        const val LOOP_NONE = "none"
        const val LOOP_YEARLY = "yearly"
        const val LOOP_MONTHLY = "monthly"
    }
}
