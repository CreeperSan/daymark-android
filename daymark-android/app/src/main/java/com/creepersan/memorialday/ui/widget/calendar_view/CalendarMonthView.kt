package com.creepersan.memorialday.ui.widget.calendar_view

import android.content.Context
import android.util.AttributeSet
import android.widget.GridLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.creepersan.memorialday.R
import com.creepersan.memorialday.databinding.ViewCalendarMonthBinding
import com.creepersan.memorialday.extension.safeGet

class CalendarMonthView: GridLayout {
    private val binding: ViewCalendarMonthBinding
    private val dayViewArray: Array<Array<CalendarDayView>>
    private var onDayCLickListener: OnDayClickListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        inflate(context, R.layout.view_calendar_month, this)
        binding = ViewCalendarMonthBinding.bind(this)
        dayViewArray = arrayOf(
            arrayOf( binding.day11, binding.day12, binding.day13, binding.day14, binding.day15, binding.day16, binding.day17 ),
            arrayOf( binding.day21, binding.day22, binding.day23, binding.day24, binding.day25, binding.day26, binding.day27 ),
            arrayOf( binding.day31, binding.day32, binding.day33, binding.day34, binding.day35, binding.day36, binding.day37 ),
            arrayOf( binding.day41, binding.day42, binding.day43, binding.day44, binding.day45, binding.day46, binding.day47 ),
            arrayOf( binding.day51, binding.day52, binding.day53, binding.day54, binding.day55, binding.day56, binding.day57 ),
        )

        dayViewArray.forEachIndexed { y, columnArray ->
            columnArray.forEachIndexed { x, calendarDayView ->
                calendarDayView.setOnClickListener {
                    onDayCLickListener?.onClick(x, y, calendarDayView)
                }
            }
        }

    }


    fun getDayView(x: Int, y: Int): CalendarDayView? {
        return dayViewArray.safeGet(y)?.safeGet(x)
    }


    fun setOnDayClickListener(listener: OnDayClickListener?) {
        onDayCLickListener = listener
    }

    interface OnDayClickListener {

        fun onClick(x: Int, y: Int, view: CalendarDayView)

    }

}