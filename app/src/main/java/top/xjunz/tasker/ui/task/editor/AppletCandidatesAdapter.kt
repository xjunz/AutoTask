package top.xjunz.tasker.ui.task.editor

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemAppletInShopCartBinding
import top.xjunz.tasker.engine.criterion.Criterion
import top.xjunz.tasker.engine.criterion.PropertyCriterion
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.factory.AppletRegistry
import top.xjunz.tasker.ui.ColorSchemes
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(private val appletRegistry: AppletRegistry) :
    RecyclerView.Adapter<AppletCandidatesAdapter.AppletViewHolder>() {

    private val applets = mutableListOf<Applet>()

    fun updateApplets(data: List<Flow>) {
        val oldData = ArrayList(applets)
        applets.clear()
        applets.addAll(data.flatMap {
            Collections.singleton(it) + it.applets
        })
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldData.size
            }

            override fun getNewListSize(): Int {
                return applets.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldData[oldItemPosition] === applets[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldData[oldItemPosition] === applets[newItemPosition]
            }
        }
        DiffUtil.calculateDiff(diffCallback).dispatchUpdatesTo(this)
    }


    inner class AppletViewHolder(val binding: ItemAppletInShopCartBinding) :
        ViewHolder(binding.root) {
        init {
            binding.tvTitle.setOnClickListener {
                applets[adapterPosition].switchRelation()
                notifyItemChanged(adapterPosition, true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppletViewHolder {
        return AppletViewHolder(
            ItemAppletInShopCartBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    private fun makeRelationSpan(origin: CharSequence, relation: Int): CharSequence {
        val relationText =
            if (relation == Applet.RELATION_OR) R.string.flow_or.text else R.string.flow_and.text
        return SpannableStringBuilder().append(
            relationText,
            ForegroundColorSpan(ColorSchemes.colorTextLink),
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        ).append(origin).also {
            it.setSpan(
                UnderlineSpan(), 0, 1,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.setSpan(
                StyleSpan(Typeface.BOLD), 0, 1,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    }

    override fun onBindViewHolder(holder: AppletViewHolder, position: Int) {
        val applet = applets[position]
        holder.binding.let {
            val showRelation = position != 0 && applets[position - 1] !is Flow
            val option = appletRegistry.findOption(applet)
            val originTitle = option.currentTitle
            if (originTitle != null && showRelation && applet.relation != Applet.RELATION_NONE) {
                it.tvTitle.text = makeRelationSpan(originTitle, applet.relation)
            } else {
                it.tvTitle.text = originTitle
            }
            it.tvDesc.isVisible = applet !is Flow
            if (applet is Flow) {
                it.ibAction.setImageResource(R.drawable.ic_baseline_expand_more_24)
                it.tvTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
                it.tvTitle.setTextColor(ColorSchemes.colorOnSurface)
            } else {
                it.tvDesc.isVisible = applet !is PropertyCriterion<*>
                it.ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                it.tvTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                it.tvTitle.setTextColor(ColorSchemes.textColorTertiary)
                if (applet is Criterion<*, *>) {
                    it.tvDesc.text = option.getDescription(applet.requireValue())
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return applets.size
    }
}