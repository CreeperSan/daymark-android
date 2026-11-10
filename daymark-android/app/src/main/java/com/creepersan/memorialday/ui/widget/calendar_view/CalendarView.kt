package com.creepersan.memorialday.ui.widget.calendar_view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import com.creepersan.memorialday.R
import com.creepersan.memorialday.databinding.ViewCalendarBinding
import com.creepersan.memorialday.extension.disposeIfNeed
import com.creepersan.memorialday.extension.safeGet
import com.creepersan.memorialday.extension.setVisible
import com.creepersan.memorialday.module.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.module.chinese_calendar.model.CalendarMonth
import com.creepersan.memorialday.module.chinese_calendar.model.CalendarYear
import com.creepersan.memorialday.module.chinese_calendar.model.ChineseCalendar
import com.creepersan.memorialday.module.chinese_calendar.model.lunarDayToString
import com.creepersan.memorialday.module.chinese_calendar.model.lunarMonthToString
import com.google.android.material.color.MaterialColors
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
            com.creepersan.memorialday.module.log.Log.info("日历切换到上个月", "CalendarWidget")
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
            com.creepersan.memorialday.module.log.Log.info("日历切换到下个月", "CalendarWidget")
            if (currentMonth >= 12) {
                currentMonth = 1
                currentYear += 1
            } else {
                currentMonth += 1
            }
            loadCalendar(year=currentYear, month=currentMonth, showLoading = false)
        }

        // 日期区域
        binding.timeClickZone.setOnClickListener {
            com.creepersan.memorialday.module.log.Log.info("打开日历日期选择器", "CalendarWidget")
            toggleShowDatePicker()
            initializePicker()
        }

        // 日期区域动画
        animateDatePicker.duration = 300
        animateDatePicker.addUpdateListener {
            val progress = it.animatedValue as Float
            binding.pickerLayout.alpha = progress
            binding.pickerLayout.translationY = (-1 + progress) * binding.pickerLayout.height
            binding.pickerLayout.setVisible(progress > 0)
            binding.datePickerIndicator.rotation = progress * 180
        }

        // 日期选择确认按钮
        binding.btnDatePickerConfirm.setOnClickListener {
            com.creepersan.memorialday.module.log.Log.info("确认日历日期", "CalendarWidget")
            confirmDatePickerValue()
        }

    }


    fun confirmDatePickerValue() {
        if (isShowDatePickerLayout()) {
            toggleShowDatePicker()
            loadCalendar(cacheYear, cacheMonth)
        }
    }


    private val animateDatePicker = ValueAnimator.ofFloat(0f, 1f)
    private var isShowingDatePickerLayout = false;
    fun toggleShowDatePicker() {
        if (animateDatePicker.isRunning) {
            animateDatePicker.cancel()
        }

        if (isShowingDatePickerLayout) { // 正在展示，需要取消展示
            animateDatePicker.reverse()
        } else { // 没有展示，需要展示
            animateDatePicker.start()
        }

        isShowingDatePickerLayout = !isShowingDatePickerLayout
    }

    fun isShowDatePickerLayout() = isShowingDatePickerLayout


    var disposableYear: Disposable? = null
    var disposableMonth: Disposable? = null
    var cacheYear = 0
    var cacheMonth = 0
    private fun initializePicker() {
        cacheYear = currentYear
        cacheMonth = currentMonth

        val dataYearList = ArrayList<CalendarYear>() // 年列表
        val dataMonthList = ArrayList<CalendarMonth>() // 月列表

        fun loadMonth() {
            disposableMonth.disposeIfNeed()
            disposableMonth = ChineseCalendarUtils.getAvailableMonth(cacheYear).subscribe { monthList ->
                if (monthList.isEmpty()) {
                    return@subscribe
                }
                dataMonthList.clear()
                dataMonthList.addAll(monthList)
                binding.pickerMonth.minValue = 0
                binding.pickerMonth.maxValue = monthList.size - 1
                binding.pickerMonth.displayedValues = monthList.map { it.month.toString() }.toTypedArray()
                binding.pickerMonth.value = monthList.indexOfFirst { it.month == cacheMonth }
            }
        }

        fun loadYear() {
            disposableYear.disposeIfNeed()
            disposableYear = ChineseCalendarUtils.getAvailableYear().subscribe { yearList ->
                if (yearList.isEmpty()) {
                    return@subscribe
                }

                yearList.removeIf { it.year < 1910 || it.year > 2090 }

                dataYearList.clear()
                dataYearList.addAll(yearList)

                binding.pickerYear.minValue = 0
                binding.pickerYear.maxValue = yearList.size - 1
                binding.pickerYear.displayedValues = yearList.map { it.year.toString() }.toTypedArray()
                binding.pickerYear.value = yearList.indexOfFirst { it.year == cacheYear }

                loadMonth()
            }
        }

        // 选择器数据事件初始化
        binding.pickerYear.wrapSelectorWheel = false
        binding.pickerYear.setOnValueChangedListener { picker, oldVal, newVal ->
            val selectYear = dataYearList.safeGet(newVal)?.year ?: return@setOnValueChangedListener
            cacheYear = selectYear
            loadYear()
        }
        binding.pickerMonth.wrapSelectorWheel = false
        binding.pickerMonth.setOnValueChangedListener { picker, oldVal, newVal ->
            val selectMonth = dataMonthList.safeGet(newVal)?.month ?: return@setOnValueChangedListener
            cacheMonth = selectMonth
        }

        loadYear()
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

        val enableRight = loadYear < 2090 || (loadYear == 2090 && loadMonth < 12)
        val enableLeft = loadYear > 1910 || (loadYear == 1910 && loadMonth > 1)

        binding.rightArrow.isInvisible = !enableRight

        binding.leftArrow.isInvisible = !enableLeft

        if (loadYear >= 2100 || (loadYear == 2099 && loadMonth == 12)) {
            return
        }

        if (loadYear <= 1900 || (loadYear == 1901 && loadMonth == 1)) {
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
                    val descriptionColor = MaterialColors.getColor(dayView, R.attr.md_color_primary_darken)
                    dayView.setDescriptionTextColor(descriptionColor)
                    dayView.setDescription(calendar.lunarMonthToString())
                } else {
                    val descriptionColor = MaterialColors.getColor(dayView, R.attr.md_color_text_primary)
                    dayView.setDescriptionTextColor(descriptionColor)
                    dayView.setDescription(calendar.lunarDayToString())
                }
                // 点击事件
                dayView.setOnClickListener {
                    com.creepersan.memorialday.module.log.Log.info("选择日历日期", "CalendarWidget")
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



