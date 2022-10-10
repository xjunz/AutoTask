package top.xjunz.tasker.ui.task.editor.selector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import top.xjunz.tasker.databinding.ItemActivityInfoBinding
import top.xjunz.tasker.ktx.require

/**
 * @author xjunz 2022/10/09
 */
class ActivityInfoAdapter(
    private val viewModel: ComponentSelectorViewModel,
    private val activitiesInfo: List<ActivityInfoWrapper>,
    private val host: ComponentSelectorDialog
) : RecyclerView.Adapter<ActivityInfoAdapter.ActivityInfoViewHolder>() {

    inner class ActivityInfoViewHolder(val binding: ItemActivityInfoBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                host.onActivityItemClicked(activitiesInfo[adapterPosition], binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityInfoViewHolder {
        return ActivityInfoViewHolder(
            ItemActivityInfoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ActivityInfoViewHolder, position: Int) {
        val pkgInfo = viewModel.selectedPackage.require()
        val actInfo = activitiesInfo[position]
        holder.binding.let {
            viewModel.iconLoader.loadIconTo(pkgInfo, it.ivIcon, host)
            it.tvActivityName.text = actInfo.label
            it.tvFullName.text = actInfo.source.name
            it.tvBadge.isVisible = actInfo.isEntrance
            it.root.isSelected = viewModel.selectedActivities.contains(actInfo.componentName)
        }
    }

    override fun getItemCount(): Int {
        return activitiesInfo.size
    }
}