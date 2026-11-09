package com.creepersan.memorialday.ui.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.creepersan.memorialday.R
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar.MAX_YEAR
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar.MIN_YEAR
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar.MONTHS_PER_YEAR
import com.creepersan.memorialday.module.anniversary.CalendarDate
import com.creepersan.memorialday.module.anniversary.LunarDateLabels
import kotlin.math.abs

private val DatePickerRowHeight = 44.dp
private val DatePickerMinimumRowHeight = 1.dp
private val DatePickerHeaderHeight = 44.dp
// GlassCard 的内外纵向留白合计为 52.dp，另加标题和滚轮之间的间隔。
private val DatePickerChromeHeight = DatePickerHeaderHeight + 60.dp

/** 公历日期选择入口，弹层内只处理公历日期。 */
@Composable
fun GlassSolarDatePicker(date: CalendarDate, onDismiss: () -> Unit, onConfirm: (CalendarDate) -> Unit) {
    GlassDatePicker(date, lunar = false, chineseLunar = false, onDismiss, onConfirm)
}

/** 农历日期选择入口，闰月以独立月份插入对应的月份滚轮。 */
@Composable
fun GlassLunarDatePicker(date: CalendarDate, onDismiss: () -> Unit, onConfirm: (CalendarDate) -> Unit) {
    val language = LocalConfiguration.current.locales[0].language
    GlassDatePicker(date, lunar = true, chineseLunar = language == "zh" || language == "yue", onDismiss, onConfirm)
}

/** 两种历法共用滚轮布局，月份及天数由各自的日历规则决定。 */
@Composable
private fun GlassDatePicker(date: CalendarDate, lunar: Boolean, chineseLunar: Boolean, onDismiss: () -> Unit, onConfirm: (CalendarDate) -> Unit) {
    val initialYear = date.year.coerceIn(MIN_YEAR, MAX_YEAR)
    val initialMonth = date.month.coerceIn(1, MONTHS_PER_YEAR)
    var year by remember { mutableStateOf(initialYear) }
    var month by remember { mutableStateOf(if (lunar && date.leap) -initialMonth else initialMonth) }
    val yearState = rememberLazyListState(initialYear - MIN_YEAR)
    val months = remember(year, lunar) { AnniversaryCalendar.monthsInYear(year, lunar) }
    // 保留月份本身，避免插入闰月时把之后的月份按旧索引错位。闰月消失则退回同名平月。
    val selectedMonth = if (month in months) month else abs(month)
    val monthState = key(months) { rememberLazyListState(months.indexOf(selectedMonth)) }
    val dayCount = remember(year, selectedMonth, lunar) { AnniversaryCalendar.daysInMonth(year, selectedMonth, lunar) }
    // 月份缩短时，列表会把当前位置限制到新的最后一日。
    val dayState = rememberLazyListState(date.day.coerceIn(1, dayCount) - 1)
    val settledYear = if (yearState.isScrollInProgress) null else yearState.centeredWheelIndex()?.plus(MIN_YEAR)
    val settledMonth = if (monthState.isScrollInProgress) null else monthState.centeredWheelIndex()?.let(months::getOrNull)
    LaunchedEffect(settledYear) { if (settledYear != null) year = settledYear }
    LaunchedEffect(monthState, settledMonth) { if (settledMonth != null) month = settledMonth }

    val settledDay = if (dayState.isScrollInProgress || dayState.layoutInfo.totalItemsCount != dayCount) null else dayState.centeredWheelIndex()?.plus(1)
    // 等年月联动刷新、三个滚轮吸附完成再确认，避免保存上一个月的日期。
    val canConfirm = settledYear == year && settledMonth == selectedMonth && settledDay != null
    val title = stringResource(if (lunar) R.string.anniversary_lunar else R.string.anniversary_solar)
    val leapPrefix = stringResource(R.string.anniversary_leap_prefix)
    // 弹窗是独立窗口，使用宿主页的安全区域，防止最后一行与三键导航重叠。
    val screenInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(screenInsets).padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
            val rowHeight = ((maxHeight - DatePickerChromeHeight) / WHEEL_VISIBLE_ROWS).coerceIn(DatePickerMinimumRowHeight, DatePickerRowHeight)
            GlassCard(solid = true) {
                Row(Modifier.fillMaxWidth().height(DatePickerHeaderHeight), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.fillMaxHeight().glassClickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.fillMaxHeight().glassClickable(enabled = canConfirm) {
                        if (settledDay != null) onConfirm(CalendarDate(year, abs(selectedMonth), settledDay, selectedMonth < 0))
                    }, contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.confirm), color = if (canConfirm) MaterialTheme.colorScheme.primary else glassSecondaryColor())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    GlassWheelPicker(MAX_YEAR - MIN_YEAR + 1, yearState, rowHeight, Modifier.weight(1f)) { (it + MIN_YEAR).toString() }
                    GlassWheelPicker(months.size, monthState, rowHeight, Modifier.weight(1f)) { index ->
                        val value = months[index]
                        val label = if (chineseLunar) LunarDateLabels.month(abs(value)) else abs(value).toString()
                        if (value < 0) leapPrefix + label else label
                    }
                    GlassWheelPicker(dayCount, dayState, rowHeight, Modifier.weight(1f)) { if (chineseLunar) LunarDateLabels.day(it + 1) else (it + 1).toString() }
                }
            }
        }
    }
}
