package com.creepersan.memorialday.ui.dialog.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.creepersan.memorialday.R
import com.creepersan.memorialday.databinding.DialogBaseBinding
import com.creepersan.memorialday.extension.setVisible
import io.reactivex.rxjava3.core.Observable

open class BaseDialog(
    context: Context,
    private val fullscreen: Boolean = false,
    /* 标题 */
    private val title: String? = null,
    private val titleSize: Float? = null,
    @ColorInt private val titleColor: Int? = null,
    /* 副标题 */
    private val subtitle: String? = null,
    private val subtitleSize: Float? = null,
    @ColorInt private val subtitleColor: Int? = null,
    /* 确定按钮 */
    private val positiveButton: String? = null,
    private val positiveButtonSize: Float? = null,
    @ColorInt private val positiveButtonColor: Int? = null,
    private val positiveButtonAction: (() -> Unit)? = null,
    /* 其他按钮 */
    private val neutralButton: String? = null,
    private val neutralButtonSize: Float? = null,
    @ColorInt private val neutralButtonColor: Int? = null,
    private val neutralButtonAction: (() -> Unit)? = null,
    /* 取消按钮 */
    private val negativeButton: String? = null,
    private val negativeButtonSize: Float? = null,
    @ColorInt private val negativeButtonColor: Int? = null,
    private val negativeButtonAction: (() -> Unit)? = null,
    /* 自定义内容 */
    private val customView: View? = null
) : Dialog(context, R.style.MemorialDayDialog) {
    protected lateinit var rootDialogBinding: DialogBaseBinding

    private var mCloseAction = DialogAction.Cancel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootDialogBinding = DialogBaseBinding.inflate(
            LayoutInflater.from(context)
        )

        setContentView(rootDialogBinding.root)

        /* 去除默认边框 */
        val dialogWindow = window
        if (dialogWindow != null) {
            dialogWindow.decorView.setPadding(0, 0, 0, 0)
            if (fullscreen) {
                val dialogWindowAttribute = dialogWindow.attributes
                dialogWindowAttribute.width = LinearLayout.LayoutParams.WRAP_CONTENT
                dialogWindowAttribute.height = LinearLayout.LayoutParams.WRAP_CONTENT
                dialogWindow.attributes = dialogWindowAttribute
            }
        }

        /* 标题 */
        rootDialogBinding.title.setVisible(title != null)
        rootDialogBinding.title.text = title ?: ""
        titleColor?.apply { rootDialogBinding.title.setTextColor(this) }
        titleSize?.apply { rootDialogBinding.title.setTextSize(TypedValue.COMPLEX_UNIT_PX, this)}

        /* 副标题 */
        rootDialogBinding.subtitle.setVisible(subtitle != null)
        rootDialogBinding.subtitle.text = subtitle ?: ""
        subtitleColor?.apply { rootDialogBinding.subtitle.setTextColor(this) }
        subtitleSize?.apply { rootDialogBinding.subtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, this)}

        /* 确认按钮 */
        rootDialogBinding.positive.setVisible((positiveButton ?: "").isNotEmpty())
        rootDialogBinding.positive.text = positiveButton ?: ""
        positiveButtonColor?.apply { rootDialogBinding.positive.setTextColor(this) }
        positiveButtonSize?.apply { rootDialogBinding.positive.setTextSize(TypedValue.COMPLEX_UNIT_PX, this)}
        rootDialogBinding.positive.setOnClickListener {
            mCloseAction = DialogAction.Positive
            positiveButtonAction?.invoke()
            dismiss()
        }

        /* 其他按钮 */
        rootDialogBinding.neutral.setVisible((neutralButton ?: "").isNotEmpty())
        rootDialogBinding.neutral.text = neutralButton ?: ""
        neutralButtonColor?.apply { rootDialogBinding.neutral.setTextColor(this) }
        neutralButtonSize?.apply { rootDialogBinding.neutral.setTextSize(TypedValue.COMPLEX_UNIT_PX, this)}
        rootDialogBinding.neutral.setOnClickListener {
            mCloseAction = DialogAction.Neutral
            neutralButtonAction?.invoke()
            dismiss()
        }

        /* 取消按钮 */
        rootDialogBinding.negative.setVisible((negativeButton ?: "").isNotEmpty())
        rootDialogBinding.negative.text = negativeButton ?: ""
        negativeButtonColor?.apply { rootDialogBinding.negative.setTextColor(this) }
        negativeButtonSize?.apply { rootDialogBinding.negative.setTextSize(TypedValue.COMPLEX_UNIT_PX, this)}
        rootDialogBinding.negative.setOnClickListener {
            mCloseAction = DialogAction.Negative
            negativeButtonAction?.invoke()
            dismiss()
        }

        /* 自定义内容 */
        customView?.apply {
            rootDialogBinding.customLayout.addView(this, 0, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            ))
        }

    }


    fun showObservable(): Observable<DialogAction> {
        return Observable.create { emitter ->
            show()
            setOnDismissListener {
                emitter.onNext(mCloseAction)
                emitter.onComplete()
            }
        }
    }

}


enum class DialogAction {
    Positive,
    Negative,
    Neutral,
    Cancel,
}
