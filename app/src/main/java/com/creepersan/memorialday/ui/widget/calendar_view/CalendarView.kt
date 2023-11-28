package com.creepersan.memorialday.ui.widget.calendar_view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import com.creepersan.memorialday.R
import com.creepersan.memorialday.databinding.ViewCalendarBinding
import com.creepersan.memorialday.extension.disposeIfNeed
import com.creepersan.memorialday.extension.safeGet
import com.creepersan.memorialday.extension.setVisible
import com.creepersan.memorialday.utils.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import com.creepersan.memorialday.utils.chinese_calendar.model.lunarDayToString
import com.creepersan.memorialday.utils.chinese_calendar.model.lunarMonthToString
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Calendar

class CalendarView : ConstraintLayout {
    private val binding: ViewCalendarBinding
    private var currentYear: Int    // 当前的年
    private var currentMonth: Int   // 当前的月，1 代表 一月
    private val currentCalendarList = ArrayList<ChineseCalendar>()  // 当前的日历列表

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.view_calendar, this)
        binding = ViewCalendarBinding.bind(this)

        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)

        // 上一个月按钮点击事件
        binding.leftArrow.setOnClickListener {
            if (currentMonth <= 1) {
                currentYear -= 1
                currentMonth = 12
            } else {
                currentMonth -= 1
            }
            loadCalendar(year=currentYear, month=currentMonth)
        }

        // 下一个月按钮点击事件
        binding.rightArrow.setOnClickListener {
            if (currentMonth >= 12) {
                currentMonth = 1
                currentYear += 1
            } else {
                currentMonth += 1
            }
            loadCalendar(year=currentYear, month=currentMonth, showLoading = false)
        }

    }


    // 当前选中的日期
    private var selectionYear = 0
    private var selectionMonth = 0
    private var selectionDay = 0


    var mLoadSelectionLunarNameDisposable: Disposable? = null
    fun setSelection(year: Int, month: Int, day: Int) {
        selectionYear = year
        selectionMonth = month
        selectionDay = day
        mLoadSelectionLunarNameDisposable?.dispose()
        mLoadSelectionLunarNameDisposable = ChineseCalendarUtils.getFromCalendar(year, month, day).subscribe({ chineseCalendarOptional ->
            if (chineseCalendarOptional.isPresent) {
                val chineseCalendar = chineseCalendarOptional.get()
                binding.lunarText.text = "${chineseCalendar.lunarYearName}年 ${chineseCalendar.lunarMonthToString()}${chineseCalendar.lunarDayToString()}"
            }
        }, { error ->
            error.printStackTrace()
        })
        loadCalendar(currentYear, currentMonth, showLoading = false) // FIXME UI刷新，性能消耗过大，后面要优化
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// UI相关


    private fun showAsLoading() {
        binding.calendarMonthView.isInvisible = false
        binding.progressBar.setVisible(true)
    }

    private fun showAsCalendar() {
        binding.calendarMonthView.isInvisible = false
        binding.progressBar.setVisible(false)
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// 提供给外部调用的方法


    private var loadCalendarDisposable: Disposable? = null
    /**
     * 加载日历数据
     * @param year 年
     * @param month 月，1代表一月
     * @param showLoading 是否显示加载中
     */
    fun loadCalendar(year: Int? = null, month: Int? = null, showLoading: Boolean = true) {
        val loadYear = year ?: currentYear
        val loadMonth = month ?: currentMonth

        if (loadYear >= 2100 || loadYear <= 1900) {
            return
        }

        if (showLoading) {
            showAsLoading()
        }

        currentYear = loadYear
        currentMonth = loadMonth

        val tmpCalendar = Calendar.getInstance()

        val todayYear = tmpCalendar.get(Calendar.YEAR)
        val todayMonth = tmpCalendar.get(Calendar.MONTH) + 1
        val todayDay = tmpCalendar.get(Calendar.DAY_OF_MONTH)

        tmpCalendar.firstDayOfWeek = Calendar.SUNDAY
        tmpCalendar.set(Calendar.YEAR, loadYear)
        tmpCalendar.set(Calendar.MONTH, loadMonth - 1)
        tmpCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val calendarWeekday = tmpCalendar.get(Calendar.DAY_OF_WEEK)
        when (calendarWeekday) {
            Calendar.SUNDAY -> Unit
            Calendar.MONDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -1)
            Calendar.TUESDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -2)
            Calendar.WEDNESDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -3)
            Calendar.THURSDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -4)
            Calendar.FRIDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -5)
            Calendar.SATURDAY -> tmpCalendar.add(Calendar.DAY_OF_MONTH, -6)
        }
        val targetYear = tmpCalendar.get(Calendar.YEAR)
        val targetMonth = tmpCalendar.get(Calendar.MONTH) + 1
        val targetDay = tmpCalendar.get(Calendar.DAY_OF_MONTH)

        loadCalendarDisposable?.disposeIfNeed()
        loadCalendarDisposable = ChineseCalendarUtils.getDaysFromSpecifyWithCount(targetYear, targetMonth, targetDay, 35).subscribe({ calendarList ->
            // 数据更新
            currentCalendarList.clear()
            currentCalendarList.addAll(calendarList)
            // 设置年月
            binding.yearText.text = "${loadYear}年"
            binding.monthText.text = "${loadMonth}月"
            // 设置日历
            calendarList.forEachIndexed { listIndex, calendar ->
                val dayView = binding.calendarMonthView.getDayView(listIndex % 7, listIndex / 7)
                dayView ?: return@forEachIndexed
                // 日期
                dayView.setDayText(calendar.day.toString())
                // 隐藏标签
                dayView.setHintVisible(false)
                // 非本月要显示淡一些
                if (calendar.year == loadYear && calendar.month == loadMonth) {
                    dayView.showAsNormal()
                } else {
                    dayView.showAsWeaken()
                }
                // 第二行
                if (calendar.lunarDay == 1) {
                    dayView.setDescriptionTextColor(Color.RED)
                    dayView.setDescription(calendar.lunarMonthToString())
                } else {
                    dayView.setDescriptionTextColor(Color.BLACK)
                    dayView.setDescription(calendar.lunarDayToString())
                }
                // 点击事件
                dayView.setOnClickListener {
                    val chineseCalendar = currentCalendarList.safeGet(listIndex) ?: return@setOnClickListener
                    onDayClickListener?.invoke(chineseCalendar)
                }
                // 背景 - 已选中
                val valueIsSelected = calendar.year == selectionYear && calendar.month == selectionMonth && calendar.day == selectionDay
                dayView.setSelectionBackgroundVisible(valueIsSelected)
                // 背景 - 今天
                val valueIsToday = calendar.year == todayYear && calendar.month == todayMonth && calendar.day == todayDay
                dayView.setTodayBackgroundVisible(valueIsToday)

            }
            showAsCalendar()
        }, { error ->
            error.printStackTrace()
        })
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////  事件监听相关
    private var onDayClickListener: OnDayClickListener? = null
    fun setOnDayClickListener(listener: OnDayClickListener?) {
        this.onDayClickListener = listener
    }

}

typealias OnDayClickListener = (day: ChineseCalendar) -> Unit



