package top.xjunz.tasker.ui.task.selector

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
import com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemAppletCandidateBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.task.editor.FlowItemTouchHelper
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(
    private val viewModel: AppletSelectorViewModel,
    private val onClickListener: AppletOptionOnClickListener
) : ListAdapter<Applet, AppletCandidatesAdapter.AppletViewHolder>(FlowItemTouchHelper.DiffCallback) {

    private val itemTouchHelperCallback = object : FlowItemTouchHelper(this) {

        override val Flow.isCollapsed: Boolean
            get() = viewModel.isCollapsed(this)

        override fun notifyFlowChanged() {
            viewModel.notifyCandidatesChanged()
        }

        override fun swapApplets(from: Applet, to: Applet) {
            if (from.parent == null) {
                viewModel.swapFlows(from.casted(), to.casted())
            } else {
                super.swapApplets(from, to)
            }
        }
    }

    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    inner class AppletViewHolder(val binding: ItemAppletCandidateBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val applet = currentList[adapterPosition]
                if (applet is Flow) {
                    viewModel.toggleCollapse(applet)
                } else {
                    onClickListener.onClick(applet) {
                        notifyItemChanged(adapterPosition, true)
                    }
                }
            }
            binding.tvTitle.setOnClickListener {
                currentList[adapterPosition].toggleRelation()
                notifyItemChanged(adapterPosition, true)
            }
            binding.ibAction.setOnClickListener {
                val applet = currentList[adapterPosition]
                if (applet is Flow) {
                    viewModel.toggleCollapse(applet)
                    notifyItemChanged(adapterPosition, true)
                    viewModel.notifyCandidatesChanged()
                } else {
                    applet.toggleInversion()
                    notifyItemChanged(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppletViewHolder {
        return AppletViewHolder(
            ItemAppletCandidateBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AppletViewHolder, position: Int) {
        val applet = currentList[position]
        holder.itemView.translationX = 0F
        holder.binding.let {
            val isFlow = applet is Flow
            val showRelation = position != 0 && applet.index != 0
            val option = viewModel.appletOptionFactory.findOption(applet)
            val title = option.getTitle(applet.isInverted)
            if (title != null && showRelation) {
                it.tvTitle.text = option.makeRelationSpan(title, applet.isAnd)
            } else {
                it.tvTitle.text = title
            }
            if (isFlow) {
                it.groupLeft.isVisible = false
                it.tvDesc.isVisible = false
                it.ibAction.isVisible = true
                if (viewModel.isCollapsed(applet as Flow)) {
                    it.ibAction.setContentDescriptionAndTooltip(R.string.expand_more.text)
                    it.ibAction.setImageResource(R.drawable.ic_baseline_expand_more_24)
                } else {
                    it.ibAction.setContentDescriptionAndTooltip(R.string.unfold_less.text)
                    it.ibAction.setImageResource(R.drawable.ic_baseline_expand_less_24)
                }
                it.tvTitle.setTextAppearance(TextAppearance_Material3_TitleLarge)
            } else {
                it.groupLeft.isVisible = true
                it.tvNumber.text = (applet.index + 1).toString()
                it.tvDesc.isVisible = applet !is PropertyCriterion<*>
                it.ibAction.isVisible = applet.isInvertible
                it.ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                it.tvTitle.setTextAppearance(TextAppearance_Material3_BodyMedium)
                if (applet is Criterion<*, *>) {
                    it.tvDesc.text = option.describe(applet.value)
                }
                if (applet.valueType == AppletValues.VAL_TYPE_TEXT) {
                    it.tvDesc.setTypeface(null, Typeface.ITALIC)
                } else {
                    it.tvDesc.setTypeface(null, Typeface.NORMAL)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}