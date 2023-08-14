package com.creepersan.memorialday.ui.system

import android.view.View
import android.view.Window

object StatusBarManager {

    enum class ContentColor {
        LIGHT,
        DARK
    }


    fun setSystemStatusBarContentColor(window: Window, color: ContentColor) {
        val decorView = window.decorView
        var systemUIVisibility = decorView.systemUiVisibility

        when (color) {
            ContentColor.LIGHT -> {
                systemUIVisibility = systemUIVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            ContentColor.DARK -> {
                systemUIVisibility = systemUIVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        decorView.systemUiVisibility = systemUIVisibility
    }


}