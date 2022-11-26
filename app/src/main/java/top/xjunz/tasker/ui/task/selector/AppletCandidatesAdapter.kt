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
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemAppletCandidateBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.task.editor.FlowItemTouchHelperCallback
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(
    private val viewModel: AppletSelectorViewModel,
    private val onClickListener: AppletOptionOnClickListener,
) : ListAdapter<Applet, AppletCandidatesAdapter.AppletViewHolder>(FlowItemTouchHelperCallback.DiffCallback) {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        ItemTouchHelper(FlowItemTouchHelperCallback(recyclerView, viewModel))
            .attachToRecyclerView(recyclerView)
    }

    inner class AppletViewHolder(val binding: ItemAppletCandidateBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val applet = currentList[adapterPosition]
                if (applet is Flow) {
                    viewModel.toggleCollapse(applet)
                } else {
                    onClickListener.onClick(binding.tvTitle.text, applet) {
                        notifyItemChanged(adapterPosition)
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
                    viewModel.notifyFlowChanged()
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
            val option = viewModel.appletOptionFactory.requireOption(applet)
            val title = option.getTitle(applet)
            if (title != null && showRelation) {
                it.tvTitle.text = AppletOption.makeRelationSpan(
                    title, applet.isAnd, viewModel.isInCriterionScope
                )
            } else {
                it.tvTitle.text = title
            }
            if (isFlow) {
                it.tvNumber.isVisible = false
                it.dividerTop.isVisible = false
                it.dividerBott.isVisible = false
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
                it.tvNumber.isVisible = true
                it.dividerTop.isVisible = true
                it.dividerBott.isVisible = applet.index != applet.parent?.lastIndex
                it.tvNumber.text = (applet.index + 1).toString()
                it.ibAction.isVisible = applet.isInvertible
                it.ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                it.tvTitle.setTextAppearance(TextAppearance_Material3_BodyMedium)
                it.tvDesc.text = option.describe(applet)
                it.tvDesc.isVisible = !it.tvDesc.text.isNullOrEmpty()
                if (applet.valueType == AppletValues.VAL_TYPE_TEXT) {
                    it.tvDesc.setTypeface(null, Typeface.ITALIC)
                } else {
                    it.tvDesc.setTypeface(null, Typeface.NORMAL)
                }
            }
        }
    }
}