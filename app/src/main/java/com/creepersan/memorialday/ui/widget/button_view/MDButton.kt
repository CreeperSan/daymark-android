package com.creepersan.memorialday.ui.widget.button_view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import com.creepersan.memorialday.extension.dp
import com.creepersan.memorialday.extension.dpi

class MDButton : AppCompatButton, View.OnTouchListener {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        setOnTouchListener(this)
    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                setPadding(paddingLeft, paddingTop + 4.dpi, paddingRight, paddingBottom)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setPadding(paddingLeft, paddingTop - 4.dpi, paddingRight, paddingBottom)
            }
        }
        return false
    }


}