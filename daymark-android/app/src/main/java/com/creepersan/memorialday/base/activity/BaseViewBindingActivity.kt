package com.creepersan.memorialday.base.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.reflect.ParameterizedType

open class BaseViewBindingActivity<T: ViewBinding>: BaseActivity() {
    protected lateinit var binding : T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewBinding()

    }

    private fun initViewBinding() {
        val type = javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val tClass = type.actualTypeArguments[0] as? Class<T>
            val tClassInstanceMethod = tClass?.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
            binding = tClassInstanceMethod?.invoke(null, layoutInflater, null, false) as T
            setContentView(binding.root)
            // 在经典页面根布局统一避开系统栏，保留布局原有内边距。
            val root = binding.root
            val left = root.paddingLeft; val top = root.paddingTop; val right = root.paddingRight; val bottom = root.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
            ViewCompat.requestApplyInsets(root)
        } else {
            finish()
        }
    }

}