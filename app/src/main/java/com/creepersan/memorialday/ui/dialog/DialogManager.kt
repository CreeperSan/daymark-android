package com.creepersan.memorialday.ui.dialog

import android.app.AlertDialog
import android.content.Context
import androidx.annotation.UiThread
import io.reactivex.rxjava3.core.Observable

object DialogManager {

    @UiThread
    fun confirm(
        context: Context,
        title: String = "",
        message: String = "",
        cancelable: Boolean = true,
        outsideTouchable: Boolean = true,
        positiveText: String = "确定",
        negativeText: String = "取消",
    ): Observable<Boolean> {
        return Observable.create { emitter ->
            val dialogBuilder = AlertDialog.Builder(context)
            if (title.isNotEmpty()) dialogBuilder.setTitle(title)
            if (message.isNotEmpty()) dialogBuilder.setMessage(message)
            dialogBuilder.setPositiveButton(positiveText) { _, _ ->
                emitter.onNext(true)
                emitter.onComplete()
            }
            dialogBuilder.setNegativeButton(negativeText) { _, _ ->
                emitter.onNext(false)
                emitter.onComplete()
            }
            dialogBuilder.setOnCancelListener { _ ->
                emitter.onNext(false)
                emitter.onComplete()
            }
            val dialog = dialogBuilder.create()
            dialog.setCancelable(cancelable)
            dialog.setCanceledOnTouchOutside(outsideTouchable)
            dialog.show()
        }
    }

}