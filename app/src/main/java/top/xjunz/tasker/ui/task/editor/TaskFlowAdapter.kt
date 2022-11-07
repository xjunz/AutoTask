package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(
    private val fragment: TaskEditorDialog,
    private val viewModel: TaskEditorViewModel
) : ListAdapter<Applet, TaskFlowAdapter.FlowViewHolder>(FlowItemTouchHelper.DiffCallback) {

    private val itemViewBinder = TaskFlowItemViewBinder(viewModel)

    private val layoutInflater = LayoutInflater.from(fragment.requireContext())

    private lateinit var recyclerView: RecyclerView

    private val itemTouchHelperCallback = object : FlowItemTouchHelper(this) {

        override val Flow.isCollapsed: Boolean
            get() = viewModel.isCollapsed(this)

        override fun notifyFlowChanged() {
            viewModel.notifyFlowChanged()
        }

    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { view ->
                viewModel.singleSelect(adapterPosition)
                val popup = PopupMenu(
                    view.context, view, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
                )
                popup.menuInflater.inflate(R.menu.flow_editor, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.item_add_inside -> {
                            val flow = viewModel.selectedApplet!! as Flow
                            if (flow.size == Applet.MAX_FLOW_CHILD_COUNT) {
                                toast(R.string.reach_max_applet_size)
                                return@setOnMenuItemClickListener true
                            }
                            if (flow is ControlFlow && flow.size == flow.requiredElementCount) {
                                toast(R.string.format_reach_required_applet_size.format(flow.requiredElementCount))
                                return@setOnMenuItemClickListener true
                            }
                            AppletSelectorDialog().setTitle(item.title!!).doOnCompletion {
                                if (flow.size + it.size > Applet.MAX_FLOW_CHILD_COUNT) {
                                    toast(R.string.over_max_applet_size)
                                    return@doOnCompletion
                                }
                                flow.addAll(it)
                                viewModel.notifyFlowChanged()
                            }.show(fragment.parentFragmentManager)
                        }
                        R.id.item_add_after -> {

                        }
                        R.id.item_add_before -> {

                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                popup.setOnDismissListener {
                    viewModel.singleSelect(adapterPosition)
                }
                popup.show()
            }
            binding.tvTitle.setOnClickListener {
                currentList[adapterPosition].toggleRelation()
                notifyItemChanged(adapterPosition, true)
            }
            binding.ibAction.setOnClickListener {
                val applet = currentList[adapterPosition]
                when (it.tag as? Int) {
                    TaskFlowItemViewBinder.ACTION_COLLAPSE -> {
                        viewModel.toggleCollapse(applet)
                        notifyItemChanged(adapterPosition)
                    }
                    TaskFlowItemViewBinder.ACTION_INVERT -> {
                        currentList[adapterPosition].toggleInversion()
                        notifyItemChanged(adapterPosition, true)
                    }
                    TaskFlowItemViewBinder.ACTION_ENTER -> {

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