package top.xjunz.tasker.ui.task.editor

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowCascadeBinding
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.controlFlow
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.option.AppletOptionFactory


/**
 * @author xjunz 2022/12/13
 */
class FlowCascadeAdapter(private val viewModel: FlowEditorViewModel) :
    RecyclerView.Adapter<FlowCascadeAdapter.FlowCascadeViewHolder>() {

    private lateinit var context: Context

    private val records = generateRecords()

    private fun generateRecords(): List<CharSequence> {
        val ret = mutableListOf<CharSequence>()
        var flow: Flow? = viewModel.flow
        while (flow != null && flow !is RootFlow) {
            if (flow.comment != null) {
                ret.add(flow.comment!!)
            } else {
                if (flow.isContainer) {
                    ret.add(
                        if (flow.controlFlow is If) {
                            R.string.matches_rule_set.text
                        } else {
                            R.string.execute_rule_set.text
                        }
                    )
                } else {
                    ret.add(AppletOptionFactory.requireOption(flow).getTitle(flow)!!)
                }
            }
            flow = flow.parent
        }
        return ret.reversed()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    inner class FlowCascadeViewHolder(val binding: ItemFlowCascadeBinding) :
        ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlowCascadeViewHolder {
        return FlowCascadeViewHolder(
            ItemFlowCascadeBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FlowCascadeViewHolder, position: Int) {
        holder.binding.tvTitle.text = records[position]
        holder.binding.ivChevronRight.isVisible = position != records.lastIndex
        holder.binding.ivStart.isVisible = position == 0
    }

    override fun getItemCount(): Int {
        return records.size
    }
}