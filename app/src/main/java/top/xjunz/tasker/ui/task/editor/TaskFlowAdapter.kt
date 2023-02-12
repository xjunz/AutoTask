/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.alphaModified
import top.xjunz.tasker.ktx.notifySelfChanged
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.isDescendantOf
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(fragment: FlowEditorDialog) :
    ListAdapter<Applet, TaskFlowAdapter.FlowViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    private val viewModel: FlowEditorViewModel by fragment.viewModels()

    private val itemViewBinder = FlowItemViewBinder(viewModel)

    private val layoutInflater = LayoutInflater.from(fragment.requireContext())

    val menuHelper = AppletOperationMenuHelper(viewModel, fragment.childFragmentManager)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (viewModel.isReadyOnly) return
        ItemTouchHelper(object : FlowItemTouchHelperCallback(recyclerView, viewModel) {

            override fun onMoveEnded(hasDragged: Boolean, position: Int) {
                if (position == RecyclerView.NO_POSITION) return
                if (hasDragged) {
                    // If dragged and it's the single selection, unselect it
                    if (viewModel.selections.size != 1) return
                    val selection = currentList[position]
                    if (viewModel.selections.first() === selection)
                        viewModel.multiUnselect(selection)
                }
            }

            override fun shouldBeInvolvedInSwipe(next: Applet, origin: Applet): Boolean {
                val isSelected = viewModel.isMultiSelected(origin)
                if (isSelected) {
                    return viewModel.isMultiSelected(next) || viewModel.selections.any {
                        it.requiredIndex == -1 && it is Flow && next.isDescendantOf(it)
                    }
                }
                return super.shouldBeInvolvedInSwipe(next, origin)
            }

            override fun doRemove(parent: Flow, from: Applet): Set<Applet> {
                val removed = mutableSetOf<Applet>()
                if (viewModel.isMultiSelected(from)) {
                    viewModel.selections.forEach {
                        if (it.requiredIndex == -1) {
                            parent.remove(it)
                            removed.add(it)
                        }
                    }
                    if (viewModel.selections.removeAll(removed))
                        viewModel.selectionLiveData.notifySelfChanged()
                }
                removed.addAll(super.doRemove(parent, from))
                return removed
            }
        }).attachToRecyclerView(recyclerView)
        // Always clear single selection on attached
        viewModel.singleSelect(-1)
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvTitle.setAntiMoneyClickListener {
                if (AppletOption.deliveringAction == null) {
                    binding.root.performClick()
                }
            }
            binding.tvComment.setBackgroundColor(
                ColorScheme.colorTertiaryContainer.alphaModified(.42F)
            )
            binding.root.setOnLongClickListener { true }
            binding.root.setAntiMoneyClickListener { view ->
                val applet = currentList[adapterPosition]
                if (viewModel.isSelectingArgument && !applet.isContainer) return@setAntiMoneyClickListener
                if (viewModel.isInMultiSelectionMode) {
                    viewModel.toggleMultiSelection(applet)
                } else {
                    viewModel.singleSelect(adapterPosition)
                    val popup = menuHelper.createStandaloneMenu(view, applet)
                    popup.setOnDismissListener {
                        viewModel.singleSelect(-1)
                    }
                    popup.show()
                }
            }
            binding.ibAction.setAntiMoneyClickListener {
                val applet = currentList[adapterPosition]
                when (it.tag as? Int) {
                    FlowItemViewBinder.ACTION_COLLAPSE -> {
                        viewModel.toggleCollapse(applet)
                        notifyItemChanged(adapterPosition)
                    }
                    FlowItemViewBinder.ACTION_INVERT -> {
                        applet.toggleInversion()
                        notifyItemChanged(adapterPosition)
                    }
                    FlowItemViewBinder.ACTION_EDIT -> {
                        menuHelper.triggerMenuItem(it, applet, R.id.item_edit)
                    }
                    FlowItemViewBinder.ACTION_ADD -> {
                        menuHelper.triggerMenuItem(it, applet, R.id.item_add_inside)
                    }
                    FlowItemViewBinder.ACTION_ENTER -> {
                        menuHelper.triggerMenuItem(it, applet, R.id.item_open_in_new)
                    }
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlowViewHolder {
        return FlowViewHolder(ItemFlowItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: FlowViewHolder, position: Int) {
        itemViewBinder.bindViewHolder(holder, currentList[position])
    }
}