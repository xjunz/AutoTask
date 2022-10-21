package top.xjunz.tasker.ui.task.inspector

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import top.xjunz.tasker.databinding.ItemBreadCrumbsBinding

/**
 * @author xjunz 2022/10/10
 */
class NodeBreadCrumbsAdapter(val context: Context, val records: List<String>) :
    RecyclerView.Adapter<NodeBreadCrumbsAdapter.BreadCrumbsViewHolder>() {

    lateinit var onItemClickedListener: (position: Int) -> Unit

    inner class BreadCrumbsViewHolder(val binding: ItemBreadCrumbsBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClickedListener(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadCrumbsViewHolder {
        return BreadCrumbsViewHolder(
            ItemBreadCrumbsBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: BreadCrumbsViewHolder, position: Int) {
        holder.binding.tvTitle.text = records[position]
    }

    override fun getItemCount(): Int {
        return records.size
    }
}