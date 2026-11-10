package com.creepersan.memorialday.ui.toast

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

/** 应用内悬浮提示；跨页面切换时在新页面继续显示。 */
object ToastManager {
    private const val DURATION_MS = 2500L
    private const val HORIZONTAL_PADDING_DP = 28
    private const val VERTICAL_PADDING_DP = 18
    private const val CORNER_RADIUS_DP = 24
    private const val ELEVATION_DP = 12
    private const val TEXT_SIZE_SP = 16f
    private val BACKGROUND_COLOR = Color.rgb(244, 231, 222)
    private val TEXT_COLOR = Color.rgb(93, 67, 60)
    private val handler = Handler(Looper.getMainLooper())
    private var currentActivity: Activity? = null
    private var view: TextView? = null
    private var text: String? = null
    private var expiresAt = 0L
    private val hide = Runnable { clear() }

    /** 显示短暂提示，重复调用会替换当前内容。 */
    fun message(context: Context, message: String) {
        handler.post {
            text = message; expiresAt = SystemClock.elapsedRealtime() + DURATION_MS
            handler.removeCallbacks(hide); handler.postDelayed(hide, DURATION_MS)
            showOn(currentActivity ?: context as? Activity)
        }
    }

    /** 页面恢复时迁移尚未结束的提示。 */
    fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if (text != null && SystemClock.elapsedRealtime() < expiresAt) showOn(activity)
    }

    fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) { currentActivity = null; detach() }
    }

    private fun showOn(activity: Activity?) {
        detach()
        if (activity == null || activity.isFinishing) return
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val label = TextView(activity).apply {
            text = this@ToastManager.text
            setTextColor(TEXT_COLOR); textSize = TEXT_SIZE_SP; gravity = Gravity.CENTER
            setPadding(dp(HORIZONTAL_PADDING_DP), dp(VERTICAL_PADDING_DP), dp(HORIZONTAL_PADDING_DP), dp(VERTICAL_PADDING_DP))
            background = GradientDrawable().apply { setColor(BACKGROUND_COLOR); cornerRadius = dp(CORNER_RADIUS_DP).toFloat() }
            elevation = dp(ELEVATION_DP).toFloat()
        }
        val root = activity.window.decorView as ViewGroup
        root.addView(label, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        view = label
    }

    private fun detach() { (view?.parent as? ViewGroup)?.removeView(view); view = null }
    private fun clear() { detach(); text = null }
}
