/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemApplicationInfoBinding
import top.xjunz.tasker.ui.model.PackageInfoWrapper
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/07/03
 */
class PackageInfoAdapter(
    initial: List<PackageInfoWrapper>,
    private val viewModel: ComponentSelectorViewModel,
    private val host: ComponentSelectorDialog
) : RecyclerView.Adapter<PackageInfoAdapter.PackageInfoViewHolder>() {

    private var data = ArrayList(initial)

    private lateinit var context: Context

    inner class PackageInfoViewHolder(val binding: ItemApplicationInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.ivMore.isVisible = viewModel.mode != ComponentSelectorDialog.MODE_PACKAGE
            binding.root.setAntiMoneyClickListener {
                host.onPackageItemClicked(data[adapterPosition], binding)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageInfoViewHolder {
        return PackageInfoViewHolder(
            ItemApplicationInfoBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PackageInfoViewHolder, position: Int) {
        holder.binding.let {
            val info = data[position]
            it.tvExtraInfo.isVisible = false
            it.tvApplicationName.text = info.label
            it.tvPkgName.text = info.packageName
            viewModel.iconLoader.loadIconTo(info, it.ivIcon, host)
            it.root.isSelected = viewModel.selectedPackages.contains(info.packageName)
            it.tvBadge.isVisible = info.selectedActCount != 0
            if (info.selectedActCount != 0) {
                it.tvBadge.text =info.selectedActCount.toString()
            }
            if (viewModel.shouldAnimateListItem) {
                val staggerAnimOffsetMills = 30L
                val easeIn = AnimationUtils.loadAnimation(context, R.anim.mtrl_item_ease_enter)
                easeIn.startOffset = (staggerAnimOffsetMills + position) * position
                it.root.startAnimation(easeIn)
                if (position == 0) {
                    viewModel.viewModelScope.launch {
                        delay(staggerAnimOffsetMills)
                        viewModel.shouldAnimateListItem = false
                    }
                }
            }
        }
    }

    fun update(newData: List<PackageInfoWrapper>) {
        val old = ArrayList(data)
        data.clear()
        data.addAll(newData)
        host.lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return old.size
                    }

                    override fun getNewListSize(): Int {
                        return data.size
                    }

                    override fun areItemsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return old[oldItemPosition] == data[newItemPosition]
                    }

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return old[oldItemPosition] == data[newItemPosition]
                    }
                }, true)
            }.dispatchUpdatesTo(this@PackageInfoAdapter)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
