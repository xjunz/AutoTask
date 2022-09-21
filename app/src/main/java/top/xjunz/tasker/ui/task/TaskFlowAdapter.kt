package top.xjunz.tasker.ui.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.task.factory.AppletRegistry.description
import top.xjunz.tasker.task.factory.AppletRegistry.label
import top.xjunz.tasker.ui.ColorSchemes
import java.util.*

/**
 * @author xjunz 2022/08/14
 */
class TaskFlowAdapter(
    private val fragment: TaskEditorDialog,
    private val viewModel: TaskEditorViewModel
) : RecyclerView.Adapter<TaskFlowAdapter.FlowViewHolder>() {

    private val layoutInflater = LayoutInflater.from(fragment.requireContext())

    private lateinit var items: List<Applet>

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        fragment.observe(viewModel.selectedIndex) {
            if (it in items.indices) {
                viewModel.selectedItem.value = items[it]
            }
        }
    }

    fun setFlow(flow: Flow) {
        items = flow.applets.flatMap {
            if (it is Flow) Collections.singleton(it) + it.applets else Collections.singleton(it)
        }
    }

    inner class FlowViewHolder(val binding: ItemFlowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                notifyAppletSelected(adapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlowViewHolder {
        return FlowViewHolder(ItemFlowItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: FlowViewHolder, position: Int) {
        val item = items[position]
        val isSelected = position == viewModel.selectedIndex.require()
        holder.binding.let {
            val desc = item.description
            it.tvDesc.isVisible = desc != null
            it.tvDesc.text = desc
            it.tvLabel.text = item.label
            it.root.isSelected = isSelected
            if (item is Flow && item.javaClass != Flow::class.java) {
                it.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
                it.tvDesc.isVisible = isSelected
                it.tvLabel.setTextColor(ColorSchemes.colorPrimary)
            } else {
                it.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                it.tvLabel.setTextColor(ColorSchemes.colorOnSurface)
            }
        }
    }


    private fun notifyAppletSelected(index: Int) {
        if (index != -1) {
            val previousIndex = viewModel.selectedIndex.require()
            if (index == previousIndex) {
                viewModel.selectedIndex.value = -1
            } else {
                viewModel.selectedIndex.value = index
            }
            if (previousIndex != -1) {
                notifyItemChanged(previousIndex, true)
            }
            notifyItemChanged(index, true)
        }
    }

    fun clearSelection() {
        notifyAppletSelected(viewModel.selectedIndex.require())
    }

    fun notifyAppletSelected(applet: Applet) {
        notifyAppletSelected(items.indexOf(applet))
    }

    override fun getItemCount(): Int {
        return items.size
    }
}