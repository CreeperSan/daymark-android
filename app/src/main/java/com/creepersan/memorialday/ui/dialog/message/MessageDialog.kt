package com.creepersan.memorialday.ui.dialog.message

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import com.creepersan.memorialday.databinding.DialogBaseContentMessageBinding
import com.creepersan.memorialday.ui.dialog.base.BaseDialog

class MessageDialog(
    context: Context,

    private val message: String? = null,
    private val messageSize: Float? = null,
    private val messageColor: Int? = null,

    /* 标题 */
    title: String? = null,
    titleSize: Float? = null,
    @ColorInt titleColor: Int? = null,
    /* 副标题 */
    subtitle: String? = null,
    subtitleSize: Float? = null,
    @ColorInt subtitleColor: Int? = null,
    /* 确定按钮 */
    positiveButton: String? = null,
    positiveButtonSize: Float? = null,
    @ColorInt positiveButtonColor: Int? = null,
    positiveButtonAction: (() -> Unit)? = null,
    /* 其他按钮 */
    neutralButton: String? = null,
    neutralButtonSize: Float? = null,
    @ColorInt neutralButtonColor: Int? = null,
    neutralButtonAction: (() -> Unit)? = null,
    /* 取消按钮 */
    negativeButton: String? = null,
    negativeButtonSize: Float? = null,
    @ColorInt negativeButtonColor: Int? = null,
    negativeButtonAction: (() -> Unit)? = null,
) : BaseDialog (
    context = context,

    /* 标题 */
    title = title,
    titleSize = titleSize,
    titleColor = titleColor,
    /* 副标题 */
    subtitle = subtitle,
    subtitleSize = subtitleSize,
    subtitleColor = subtitleColor,
    /* 确定按钮 */
    positiveButton = positiveButton,
    positiveButtonSize = positiveButtonSize,
    positiveButtonColor = positiveButtonColor,
    positiveButtonAction = positiveButtonAction,
    /* 其他按钮 */
    neutralButton = neutralButton,
    neutralButtonSize = neutralButtonSize,
    neutralButtonColor = neutralButtonColor,
    neutralButtonAction = neutralButtonAction,
    /* 取消按钮 */
    negativeButton = negativeButton,
    negativeButtonSize = negativeButtonSize,
    negativeButtonColor = negativeButtonColor,
    negativeButtonAction = negativeButtonAction,
) {
    private lateinit var binding: DialogBaseContentMessageBinding

    override fun getCustomView(): View {
        binding = DialogBaseContentMessageBinding.inflate(layoutInflater)

        binding.text.text = message ?: ""
        messageColor?.apply { binding.text.setTextColor(this) }
        messageSize?.apply { binding.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, this) }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding
    }


}