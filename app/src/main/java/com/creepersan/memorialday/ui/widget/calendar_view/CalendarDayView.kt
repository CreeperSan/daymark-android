package com.creepersan.memorialday.ui.widget.calendar_view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.creepersan.memorialday.R
import com.creepersan.memorialday.databinding.ViewCalendarItemBinding
import com.creepersan.memorialday.extension.requireBooleanIfDefine
import com.creepersan.memorialday.extension.requireColorIfDefine
import com.creepersan.memorialday.extension.requireDimensionIfDefine
import com.creepersan.memorialday.extension.requireTextIfDefine
import com.creepersan.memorialday.extension.setVisible

class CalendarDayView: ConstraintLayout {

    private val binding: ViewCalendarItemBinding


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        inflate(context, R.layout.view_calendar_item, this)
        binding = ViewCalendarItemBinding.bind(this)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CalendarDayView)
            // 顶部的提示
            typedArray.requireBooleanIfDefine(R.styleable.CalendarDayView_cdv_hint_visible) {
                setHintVisible(it)
            }
            typedArray.requireColorIfDefine(R.styleable.CalendarDayView_cdv_hint_color) {
                setHintColor(it)
            }
            // 中间的文字
            typedArray.requireTextIfDefine(R.styleable.CalendarDayView_cdv_day_text) {
                setDayText(it)
            }
            typedArray.requireDimensionIfDefine(R.styleable.CalendarDayView_cdv_day_text_size) {
                setDayTextSize(it)
            }
            typedArray.requireColorIfDefine(R.styleable.CalendarDayView_cdv_day_text_color) {
                setDayTextColor(it)
            }
            // 底部的描述
            typedArray.requireTextIfDefine(R.styleable.CalendarDayView_cdv_description_text) {
                setDescription(it)
            }
            typedArray.requireDimensionIfDefine(R.styleable.CalendarDayView_cdv_description_text_size) {
                setDescriptionTextSize(it)
            }
            typedArray.requireColorIfDefine(R.styleable.CalendarDayView_cdv_description_text_color) {
                setDescriptionTextColor(it)
            }
            typedArray.recycle()
        }
    }


    fun setDayText(text: CharSequence) {
        binding.name.text = text
    }

    fun setDay(@StringRes text: Int) {
        binding.name.setText(text)
    }

    fun setDayTextSize(px: Float) {
        binding.name.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
    }

    fun setDayTextColor(@ColorInt color: Int) {
        binding.name.setTextColor(color)
    }

    fun setDayTextColorRes(@ColorRes color: Int) {
        binding.name.setTextColor(context.getColor(color))
    }

    fun setHintVisible(visible: Boolean) {
        binding.hint.setVisible(visible)
    }

    fun setHintColor(@ColorInt color: Int) {
        binding.hint.setTextColor(color)
    }

    fun setHintColorRes(@ColorRes color: Int) {
        binding.hint.setTextColor(context.getColor(color))
    }

    fun setDescription(description: String) {
        binding.description.text = description
    }

    fun setDescription(@StringRes description: Int) {
        binding.description.setText(description)
    }


    fun setDescriptionTextSize(px: Float) {
        binding.description.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
    }

    fun setDescriptionTextColor(@ColorInt color: Int) {
        binding.description.setTextColor(color)
    }

    fun setDescriptionTextColorRes(@ColorRes color: Int) {
        binding.description.setTextColor(context.getColor(color))
    }

    fun showAsWeaken() {
        binding.root.alpha = 0.3f
    }

    fun showAsNormal() {
        binding.root.alpha = 1f
    }

    fun setTodayBackgroundVisible(visible: Boolean) {
        binding.bgToday.setVisible(visible)
    }

    fun setSelectionBackgroundVisible(visible: Boolean) {
        binding.bgSelection.setVisible(visible)
    }

}