package com.creepersan.memorialday.ui.dialog.date_picker

import android.content.Context
import android.os.Bundle
import com.creepersan.memorialday.databinding.DialogDatePickerBinding
import com.creepersan.memorialday.extension.setVisible
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

typealias OnDayConfirmAction = (day: ChineseCalendar, isLunar: Boolean) -> Unit

class DatePickerDialog(
    context: Context,
    private var selectionYear: Int = 0,                                 // 当前选中公历年
    private var selectionMonth: Int = 0,                                // 当前选中公历月
    private var selectionDay: Int = 0,                                  // 当前选中公历月
    private var useLunar: Boolean = false,                              // 是否使用农历
    private val showClearButton: Boolean = true,                        // 是否显示清除按钮
    private val onDayConfirmListener : OnDayConfirmAction? = null,      // 确认选择监听
) : BottomSheetDialog(context) {
    private lateinit var binding: DialogDatePickerBinding

    private var currentSelectChineseCalendar: ChineseCalendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogDatePickerBinding.inflate(layoutInflater)

        binding.btnClear.setVisible(showClearButton)

        setContentView(binding.root)

        val calendar = Calendar.getInstance()
        var loadYear = calendar.get(Calendar.YEAR)
        var loadMonth = calendar.get(Calendar.MONTH) + 1

        if (selectionYear > 0 && selectionMonth > 0 && selectionDay > 0){
            loadYear = selectionYear
            loadMonth = selectionMonth
        }


        binding.calendarView.setSelection(selectionYear, selectionMonth, selectionDay)

        binding.calendarView.loadCalendar(loadYear, loadMonth)

        binding.calendarView.setOnDayClickListener {
            currentSelectChineseCalendar = it
            selectionYear = it.year
            selectionMonth = it.month
            selectionDay = it.day
            binding.calendarView.setSelection(selectionYear, selectionMonth, selectionDay)
        }

        binding.btnClear.setVisible(false)

        binding.btnConfirm.setOnClickListener {
            val currentChineseCalendar = currentSelectChineseCalendar
            if (currentChineseCalendar != null){
                onDayConfirmListener?.invoke(currentChineseCalendar, useLunar)
            }
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

    }

}

