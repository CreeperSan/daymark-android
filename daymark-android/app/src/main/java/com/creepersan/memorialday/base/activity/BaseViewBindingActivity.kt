package com.creepersan.memorialday.base.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
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
        } else {
            finish()
        }
    }

}