package com.creepersan.memorialday.activity.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.creepersan.memorialday.activity.MemorialDayRoute
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.base.activity.BaseViewBindingActivity
import com.creepersan.memorialday.constance.MemorialDayScheduler
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.databinding.ActivityMainBinding
import com.creepersan.memorialday.databinding.ItemMainMemorialDayBinding
import io.reactivex.rxjava3.core.Observable

class MainActivity : BaseViewBindingActivity<ActivityMainBinding>() {
    private val mAdapter = MemorialDayAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUi()
        loadData()
    }

    private fun initUi() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mAdapter

        binding.addButton.setOnClickListener {
            MemorialDayRoute.toAddMemorialDayPage(this)
        }

        binding.refreshLayout.isRefreshing = true
        binding.refreshLayout.setOnRefreshListener {
            loadData()
        }

        loadData()
    }


    private fun loadData() {
        val disposable = Observable.just(Unit).observeOn(MemorialDayScheduler.database).map {
            MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
        }.map {  dao ->
            dao.getAll()
        }.observeOn(MemorialDayScheduler.ui).subscribe({ memorialDayList ->
            mAdapter.updateList(memorialDayList)
        }, { error ->
            error.printStackTrace()
        }, {
            binding.refreshLayout.isRefreshing = false
        })
    }

}

private class MemorialDayAdapter : RecyclerView.Adapter<MemorialDayViewHolder>() {
    private val dataList = ArrayList<MemorialDay>()

    fun updateList(memorialDayList: List<MemorialDay>) {
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
    }

}

private class MemorialDayViewHolder(private val binding: ItemMainMemorialDayBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: MemorialDay) {
        binding.title.text = model.title
        binding.time.text = model.time.toString()
    }

}