package top.xjunz.tasker.ui.task.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ui.task.selector.AppletOptionOnClickListener

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(
    private val fragment: TaskEditorDialog,
    private val viewModel: TaskEditorViewModel
) : ListAdapter<Applet, TaskFlowAdapter.FlowViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    private val itemViewBinder = FlowItemViewBinder(viewModel)

    private val layoutInflater = LayoutInflater.from(fragment.requireContext())

    private lateinit var recyclerView: RecyclerView

    private val itemTouchHelperCallback = object : FlowItemTouchHelperCallback(this) {

        override val Flow.isCollapsed: Boolean
            get() = viewModel.isCollapsed(this)

        override fun notifyFlowChanged() {
            viewModel.notifyFlowChanged()
        }

    }

    private val menuHelper = FlowItemMenuHelper(viewModel, fragment)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
        // Always clear single selection on attached
        viewModel.singleSelect(-1)
    }

    private val optionOnClickListener by lazy {
        AppletOptionOnClickListener(fragment, viewModel.appletOptionFactory)
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { view ->
                val applet = currentList[adapterPosition]
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