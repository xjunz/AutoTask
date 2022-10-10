package top.xjunz.tasker.ui.task.inspector

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemNodeInfoBinding
import top.xjunz.tasker.task.inspector.StableNode

/**
 * @author xjunz 2021/9/22
 */
class NodeAdapter : RecyclerView.Adapter<NodeAdapter.NodeViewHolder>() {

    private val attrNames by lazy {
        themedContext.resources.getStringArray(R.array.node_attr_names)
    }
    private val attrMethods by lazy {
        themedContext.resources.getStringArray(R.array.node_attr_methods)
    }
    private lateinit var themedContext: Context

    fun setNode(node: StableNode) {
        this.node = node
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        themedContext = recyclerView.context
    }

    private var node: StableNode? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        return NodeViewHolder(ItemNodeInfoBinding.inflate(LayoutInflater.from(themedContext)))
    }

    private fun getAttrValue(source: AccessibilityNodeInfo, pos: Int): Any? {
        return source.javaClass.getDeclaredMethod(attrMethods[pos]).invoke(source)
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val attrName = attrNames[position]
        holder.binding.tvAttrName.text = attrName
        node?.run {
            holder.binding.tvAttrValue.text = getAttrValue(source, position)?.toString() ?: "-"
        }
    }

    override fun getItemCount() = attrNames.size

    class NodeViewHolder(internal val binding: ItemNodeInfoBinding) : RecyclerView.ViewHolder(binding.root)
}

