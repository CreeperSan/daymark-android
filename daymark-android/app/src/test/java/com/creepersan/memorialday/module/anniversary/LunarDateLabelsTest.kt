package com.creepersan.memorialday.module.anniversary

import org.junit.Assert.assertEquals
import org.junit.Test

/** 农历日期名称需正确处理初十、二十和三十等特殊日期。 */
class LunarDateLabelsTest {
    @Test fun formatsLunarDates() {
        assertEquals("正月", LunarDateLabels.month(1))
        assertEquals("腊月", LunarDateLabels.month(12))
        assertEquals("初十", LunarDateLabels.day(10))
        assertEquals("二十", LunarDateLabels.day(20))
        assertEquals("廿九", LunarDateLabels.day(29))
        assertEquals("三十", LunarDateLabels.day(30))
    }
}
