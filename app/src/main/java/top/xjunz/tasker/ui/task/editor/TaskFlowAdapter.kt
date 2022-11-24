package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.findChildWithRefid
import top.xjunz.tasker.task.applet.flatSize
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.isDescendantOf
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.selector.AppletOptionOnClickListener

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(private val fragment: FlowEditorDialog) :
    ListAdapter<Applet, TaskFlowAdapter.FlowViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    private val viewModel: FlowEditorViewModel by fragment.viewModels()

    private val itemViewBinder = FlowItemViewBinder(viewModel)

    private val layoutInflater = LayoutInflater.from(fragment.requireContext())

    private val menuHelper = FlowItemMenuHelper(viewModel, fragment)

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
                        viewModel.toggleMultiSelection(selection)
                } else {
                    val applet = currentList[position]
                    if (!viewModel.isMultiSelected(applet))
                        viewModel.toggleMultiSelection(applet)
                }
            }

            override fun shouldBeInvolvedInSwipe(next: Applet, origin: Applet): Boolean {
                val isSelected = viewModel.isMultiSelected(origin)
                if (isSelected) {
                    return viewModel.isMultiSelected(next) || viewModel.selections.any {
                        it is Flow && next.isDescendantOf(it)
                    }
                }
                return super.shouldBeInvolvedInSwipe(next, origin)
            }

            override fun doRemove(parent: Flow, from: Applet): Int {
                if (viewModel.isMultiSelected(from)) {
                    val size = viewModel.selections.flatSize
                    viewModel.selections.forEach {
                        parent.remove(it)
                    }
                    viewModel.selections.clear()
                    return size
                }
                return super.doRemove(parent, from)
            }
        }).attachToRecyclerView(recyclerView)
        // Always clear single selection on attached
        viewModel.singleSelect(-1)
    }

    private val optionOnClickListener by lazy {
        AppletOptionOnClickListener(fragment, viewModel.appletOptionFactory)
    }

    private inline fun showMultiReferenceSelectorMenu(
        anchor: View,
        candidates: List<ValueDescriptor>,
        crossinline onSelected: (ValueDescriptor) -> Unit
    ) {
        if (candidates.size == 1) {
            onSelected(candidates.single())
        } else {
            val popup = PopupMenu(fragment.requireContext(), anchor, Gravity.END)
            popup.menu.add(R.string.which_to_refer)
            candidates.forEach {
                popup.menu.add(it.name)
            }
            popup.setOnMenuItemClickListener {
                onSelected(candidates[popup.indexOf(it) - 1])
                return@setOnMenuItemClickListener true
            }
            popup.configHeaderTitle()
            popup.show()
        }
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { view ->
                val applet = currentList[adapterPosition]
                if (viewModel.isSelectingReference && !applet.isContainer) {
                    val option = viewModel.appletOptionFactory.requireOption(applet)
                    val candidates = option.results.filter {
                        viewModel.referenceToSelect.type == it.type
                    }
                    showMultiReferenceSelectorMenu(view, candidates) { candidate ->
                        val which = option.results.indexOf(candidate)
                        if (applet.referred.containsKey(which)) {
                            // Already a tag for this reference
                            viewModel.doOnRefSelected(
                                applet, which, applet.referred.getValue(which)
                            )
                            // Notify ref selected
                            viewModel.onRefSelected.value = true
                        } else {
                            TextEditorDialog().setCaption(
                                R.string.format_set_refid.formatAsHtml(candidate.name)
                            ).configEditText {
                                it.setMaxLength(Applet.Configurator.MAX_REFERRED_TAG_LENGTH)
                            }.setArguments(R.string.set_refid.text) {
                                if (it.isEmpty()) {
                                    return@setArguments R.string.error_empty_input.text
                                }
                                if (viewModel.flow.findChildWithRefid(it) != null) {
                                    return@setArguments R.string.error_tag_exists.text
                                }
                                viewModel.doOnRefSelected(applet, which, it)
                                viewModel.onRefSelected.value = true
                                return@setArguments null
                            }.show(fragment.childFragmentManager)
                        }
                    }
                    return@setOnClickListener
                }
                if (viewModel.isInMultiSelectionMode) {
                    viewModel.toggleMultiSelection(applet)
                } else if (applet is Flow && applet.isContainer) {
                    binding.ibAction.performClick()
                } else {
                    val menu = menuHelper.showMenu(view, applet)
                    if (menu != null) {
                        viewModel.singleSelect(adapterPosition)
                        menu.setOnDismissListener {
                            viewModel.singleSelect(-1)
                        }
                    } else optionOnClickListener.onClick(applet) {
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
            binding.root.setOnLongClickListener {
                if (!viewModel.isReadyOnly && !viewModel.isInMultiSelectionMode) {
                    viewModel.toggleMultiSelection(currentList[adapterPosition])
                }
                return@setOnLongClickListener true
            }
            binding.tvTitle.setOnClickListener {
                currentList[adapterPosition].toggleRelation()
                notifyItemChanged(adapterPosition, true)
            }
            binding.ibAction.setOnClickListener {
                val applet = currentList[adapterPosition]
                when (it.tag as? Int) {
                    FlowItemViewBinder.ACTION_COLLAPSE -> {
                        viewModel.toggleCollapse(applet)
                        notifyItemChanged(adapterPosition)
                    }
                    FlowItemViewBinder.ACTION_INVERT -> {
                        currentList[adapterPosition].toggleInversion()
                        notifyItemChanged(adapterPosition, true)
                    }
                    FlowItemViewBinder.ACTION_ENTER -> {
                        val dialog = FlowEditorDialog().setFlow(
                            applet as Flow, viewModel.isSelectingReference
                        ).doOnCompletion { edited ->
                            // We don't need to replace the flow, just refilling it is ok
                            applet.clear()
                            applet.addAll(edited)
                            viewModel.regenerateApplets()
                            viewModel.changedApplet.value = applet
                        }.doSplit {
                            viewModel.splitContainerFlow(applet)
                        }
                        if (viewModel.isSelectingReference) {
                            dialog.doOnReferenceSelected(viewModel.doOnRefSelected)
                            dialog.setReferenceToSelect(viewModel.referenceToSelect)
                        }
                        dialog.show(fragment.childFragmentManager)
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