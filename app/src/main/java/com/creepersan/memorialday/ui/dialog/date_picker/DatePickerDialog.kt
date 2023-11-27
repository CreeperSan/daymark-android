package com.creepersan.memorialday.ui.dialog.date_picker

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.creepersan.memorialday.constance.MemorialDayScheduler
import com.creepersan.memorialday.databinding.DialogDatePickerBinding
import com.creepersan.memorialday.ui.widget.calendar_view.CalendarMonthView
import com.creepersan.memorialday.utils.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.rxjava3.core.Observable
import java.util.Calendar

typealias OnDayClickListener = (day: ChineseCalendar) -> Unit

class DatePickerDialog(
    context: Context,
    private val selectionYear: Int = 0,
    private val selectionMonth: Int = 0,
    private val selectionDay: Int = 0,
    private val onDayClickListener : OnDayClickListener? = null,
) : BottomSheetDialog(context) {
    private lateinit var binding: DialogDatePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogDatePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        binding.calendarView.setSelection(selectionYear, selectionMonth, selectionDay)

        binding.calendarView.loadCalendar(currentYear, currentMonth)

        binding.calendarView.setOnDayClickListener {
            onDayClickListener?.invoke(it)
            dismiss()
        }
    }

}

