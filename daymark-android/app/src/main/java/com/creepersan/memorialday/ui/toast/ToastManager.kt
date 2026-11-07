package com.creepersan.memorialday.ui.toast

import android.content.Context
import android.widget.Toast

object ToastManager {

    fun message(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}