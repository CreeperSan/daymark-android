package com.creepersan.memorialday.activity.main

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.MemorialDayRoute
import com.creepersan.memorialday.activity.main.model.MemorialDayMainModel
import com.creepersan.memorialday.activity.main.viewmodel.MainUIEvent
import com.creepersan.memorialday.activity.main.viewmodel.MainViewModel
import com.creepersan.memorialday.base.activity.BaseViewBindingActivity
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.databinding.ActivityMainBinding
import com.creepersan.memorialday.databinding.ItemMainMemorialDayBinding
import com.creepersan.memorialday.extension.setVisible
import com.creepersan.memorialday.extension.spi
import com.creepersan.memorialday.ui.system.StatusBarManager
import com.creepersan.memorialday.utils.chinese_calendar.ChineseCalendarUtils
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : BaseViewBindingActivity<ActivityMainBinding>() {
    private val mAdapter = MemorialDayAdapter()
    private val viewModel by viewModels<MainViewModel>()

    private val toAddEditPage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUi()

        viewModel.loadData()
    }

    private fun initUi() {
        /* 初始化列表 */
        mAdapter.setActionListener(object : MemorialDayAdapter.ActionListener {
            override fun onItemClicked(memorialDay: MemorialDay) {
                val intent = MemorialDayRoute.toEditMemorialDayPage(this@MainActivity, memorialDay.id)
                toAddEditPage.launch(intent)
            }
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mAdapter
        lifecycleScope.launch {
            viewModel.memorialDayListChannel.consumeAsFlow().collectLatest { memorialList ->
                mAdapter.updateList(memorialList)
                binding.refreshLayout.isRefreshing = false
                binding.hintEmpty.setVisible(memorialList.isEmpty())
            }
        }

        /* 初始化添加按钮 */
        binding.addButton.setOnClickListener {
            val intent = MemorialDayRoute.toAddMemorialDayPage(this)
            toAddEditPage.launch(intent)
        }

        /* 初始化下拉刷新组件 */
        binding.refreshLayout.isRefreshing = true
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadData()
        }

        /* 初始化系统状态栏颜色 */
        StatusBarManager.setSystemStatusBarContentColor(window, StatusBarManager.ContentColor.DARK)

        lifecycleScope.launch {
            viewModel.uiChannel.consumeAsFlow().collectLatest { event ->
                when(event) {
                    is MainUIEvent.LoadDataFinish -> {
                        binding.refreshLayout.isRefreshing = false
                    }
                    else -> {}
                }
            }
        }

        viewModel.loadData()
    }

}

private class MemorialDayAdapter : RecyclerView.Adapter<MemorialDayViewHolder>() {
    private val dataList = ArrayList<MemorialDayMainModel>()
    private var actionListener: ActionListener? = null

    fun setActionListener(listener: ActionListener) {
        actionListener = listener
    }

    fun updateList(memorialDayList: List<MemorialDayMainModel>) {
        dataList.clear()
        dataList.addAll(memorialDayList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemorialDayViewHolder {
        return MemorialDayViewHolder(
            ItemMainMemorialDayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: MemorialDayViewHolder, position: Int) {
        val memorialDay = dataList[position]
        holder.bind(memorialDay)
        holder.setOnClickListener {
            actionListener?.onItemClicked(memorialDay.memorialDay)
        }
    }

    interface ActionListener {
        fun onItemClicked(memorialDay: MemorialDay)
    }

}

private class MemorialDayViewHolder(private val binding: ItemMainMemorialDayBinding) : RecyclerView.ViewHolder(binding.root) {

    @Suppress("KotlinConstantConditions")
    fun bind(model: MemorialDayMainModel) {
        val context = binding.root.context

        binding.title.text = model.title
        binding.time.text = model.time

        when {
            model.dayLeft == 0 -> {
                binding.countDown.setTextColor(MaterialColors.getColor(binding.root, R.attr.md_color_primary_darken))
                val timeStr = "就在 今天"
                binding.countDown.text = SpannableString(timeStr).apply {
                    setSpan(AbsoluteSizeSpan(20.spi), 3, timeStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            model.dayLeft == MemorialDayMainModel.DAY_LEFT_UNKNOWN -> {
                binding.countDown.setTextColor(MaterialColors.getColor(binding.root, R.attr.md_color_text_week))
                val timeStr = "未知"
                binding.countDown.text = SpannableString(timeStr).apply {
                    setSpan(AbsoluteSizeSpan(20.spi), 0, timeStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            model.dayLeft > 0 -> {
                binding.countDown.setTextColor(MaterialColors.getColor(binding.root, R.attr.md_color_text_primary))
                val timeStr = "还剩 ${model.dayLeft} 天"
                binding.countDown.text = SpannableString(timeStr).apply {
                    setSpan(AbsoluteSizeSpan(20.spi), 3, timeStr.length - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            model.dayLeft < 0 -> {
                binding.countDown.setTextColor(MaterialColors.getColor(binding.root, R.attr.md_color_text_week))
                val timeStr = "已过 ${abs(model.dayLeft)} 天"
                binding.countDown.text = SpannableString(timeStr).apply {
                    setSpan(AbsoluteSizeSpan(20.spi), 3, timeStr.length - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            else -> {
                binding.countDown.setTextColor(MaterialColors.getColor(binding.root, R.attr.md_color_text_week))
                val timeStr = "未知"
                binding.countDown.text = SpannableString(timeStr).apply {
                    setSpan(AbsoluteSizeSpan(20.spi), 0, timeStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

    }

    fun setOnClickListener(listener: View.OnClickListener) {
        binding.card.setOnClickListener(listener)
    }

}