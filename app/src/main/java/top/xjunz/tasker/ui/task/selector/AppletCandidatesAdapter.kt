/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.R.style.*
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemAppletCandidateBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.task.editor.FlowItemTouchHelperCallback
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(
    private val viewModel: AppletSelectorViewModel,
    private val onClickListener: AppletOptionClickHandler,
) : ListAdapter<Applet, AppletCandidatesAdapter.AppletViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        ItemTouchHelper(FlowItemTouchHelperCallback(recyclerView, viewModel))
            .attachToRecyclerView(recyclerView)
    }

    inner class AppletViewHolder(val binding: ItemAppletCandidateBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setAntiMoneyClickListener {
                val applet = currentList[adapterPosition]
                if (applet is Flow) {
                    if (applet.isInvertible) {
                        viewModel.toggleCollapse(applet)
                        notifyItemChanged(adapterPosition, true)
                    }
                } else {
                    onClickListener.onClick(applet) {
                        notifyItemChanged(adapterPosition, true)
                    }
                }
            }
            binding.ibAction.setAntiMoneyClickListener {
                val applet = currentList[adapterPosition]
                if (applet is Flow) {
                    viewModel.toggleCollapse(applet)
                    notifyItemChanged(adapterPosition, true)
                    viewModel.notifyFlowChanged()
                } else {
                    applet.toggleInversion()
                    notifyItemChanged(adapterPosition, true)
                }
            }
            binding.tvTitle.setAntiMoneyClickListener {
                if (AppletOption.assignedAction == null) {
                    binding.root.performClick()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppletViewHolder {
        return AppletViewHolder(
            ItemAppletCandidateBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AppletViewHolder, position: Int) {
        val applet = currentList[position]
        holder.itemView.translationX = 0F
        holder.binding.let {
            val showRelation = position != 0 && applet.index != 0
            val option = AppletOptionFactory.requireOption(applet)
            var title = if (option.descAsTitle) option.describe(applet)
            else option.loadTitle(applet)
            if (title != null && showRelation) {
                title = AppletOption.makeRelationSpan(title, applet)
            }
            if (!option.descAsTitle) {
                it.tvDesc.text = option.describe(applet)
            }
            it.tvDesc.isVisible = !it.tvDesc.text.isNullOrEmpty()
            if (applet.parent === viewModel.flow) {
                it.tvNumber.isVisible = false
                title = title?.relativeSize(1.2F)
            } else {
                it.tvNumber.isVisible = true
                it.tvNumber.text = (applet.index + 1).toString()
                it.ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
            }
            if (applet is Flow) {
                it.ibAction.isVisible = true
                if (viewModel.isCollapsed(applet)) {
                    it.ibAction.setContentDescriptionAndTooltip(R.string.expand_more.text)
                    it.ibAction.setImageResource(R.drawable.ic_baseline_expand_more_24)
                } else {
                    it.ibAction.setContentDescriptionAndTooltip(R.string.unfold_less.text)
                    it.ibAction.setImageResource(R.drawable.ic_baseline_expand_less_24)
                }
            } else {
                it.ibAction.isVisible = applet.isInvertible
                if (applet.valueType == Applet.VAL_TYPE_TEXT) {
                    it.tvDesc.setTypeface(null, Typeface.ITALIC)
                } else {
                    it.tvDesc.setTypeface(null, Typeface.NORMAL)
                }
            }
            it.tvTitle.text = title
        }
    }
}