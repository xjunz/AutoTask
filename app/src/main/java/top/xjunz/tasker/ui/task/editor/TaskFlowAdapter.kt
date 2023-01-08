/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.alphaModified
import top.xjunz.tasker.ktx.configHeaderTitle
import top.xjunz.tasker.ktx.indexOf
import top.xjunz.tasker.ktx.notifySelfChanged
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.isDescendantOf
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(private val fragment: FlowEditorDialog) :
    ListAdapter<Applet, TaskFlowAdapter.FlowViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    private val viewModel: FlowEditorViewModel by fragment.viewModels()

    private val globalViewModel: GlobalFlowEditorViewModel by fragment.activityViewModels()

    private val itemViewBinder = FlowItemViewBinder(viewModel, globalViewModel)

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

            override fun doRemove(parent: Flow, from: Applet) {
                if (viewModel.isMultiSelected(from)) {
                    val removed = mutableSetOf<Applet>()
                    viewModel.selections.forEach {
                        if (it.requiredIndex == -1) {
                            parent.remove(it)
                            removed.add(it)
                        }
                    }
                    if (viewModel.selections.removeAll(removed))
                        viewModel.selectionLiveData.notifySelfChanged()
                }
                super.doRemove(parent, from)
            }
        }).attachToRecyclerView(recyclerView)
        // Always clear single selection on attached
        viewModel.singleSelect(-1)
    }

    private inline fun showMultiReferencesSelectorMenu(
        anchor: View,
        option: AppletOption,
        candidates: List<ValueDescriptor>,
        crossinline onSelected: (Int) -> Unit
    ) {
        if (candidates.size == 1) {
            onSelected(option.results.indexOf(candidates.single()))
        } else {
            val popup = PopupMenu(fragment.requireContext(), anchor, Gravity.END)
            popup.menu.add(R.string.which_to_refer)
            candidates.forEach {
                popup.menu.add(it.name)
            }
            popup.setOnMenuItemClickListener {
                onSelected(popup.menu.indexOf(it) - 1)
                return@setOnMenuItemClickListener true
            }
            popup.configHeaderTitle()
            popup.show()
        }
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvComment.setBackgroundColor(
                ColorScheme.colorTertiaryContainer.alphaModified(.3F)
            )
            binding.root.setOnLongClickListener {
                true
            }
            binding.root.setAntiMoneyClickListener { view ->
                val applet = currentList[adapterPosition]
                if (viewModel.isSelectingRef && !applet.isContainer) {
                    if (globalViewModel.isRefSelected(applet)) {
                        globalViewModel.removeRefSelection(applet)
                        if (globalViewModel.selectedRefs.isEmpty())
                            viewModel.isFabVisible.value = false
                    } else {
                        val option = AppletOptionFactory.requireOption(applet)
                        val candidates = option.results.filter {
                            viewModel.refValueDescriptor.type == it.type
                        }
                        showMultiReferencesSelectorMenu(view, option, candidates) {
                            val refid = applet.refids[it]
                            if (refid == null) {
                                globalViewModel.addRefSelection(applet, it)
                            } else {
                                globalViewModel.addRefSelectionWithRefid(
                                    viewModel.refSelectingApplet, refid
                                )
                            }
                            if (globalViewModel.selectedRefs.isNotEmpty())
                                viewModel.isFabVisible.value = true
                        }
                    }
                } else if (viewModel.isInMultiSelectionMode) {
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
                        menuHelper.onMenuItemClick(it, applet, R.id.item_edit)
                    }
                    FlowItemViewBinder.ACTION_ADD -> {
                        menuHelper.onMenuItemClick(it, applet, R.id.item_add_inside)
                    }
                    FlowItemViewBinder.ACTION_ENTER -> {
                        menuHelper.onMenuItemClick(it, applet, R.id.item_open_in_new)
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