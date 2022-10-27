package top.xjunz.tasker.ui.task.editor

import android.graphics.Canvas
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
import com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemAppletCandidateBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.ColorSchemes
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(private val viewModel: FlowEditorViewModel) :
    RecyclerView.Adapter<AppletCandidatesAdapter.AppletViewHolder>() {

    private val applets = mutableListOf<Applet>()

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            if (viewHolder.adapterPosition == RecyclerView.NO_POSITION) return
            val applet = applets[viewHolder.adapterPosition]
            if (applet is Flow && !viewModel.isCollapsed(applet)) {
                for (i in 1..applet.count) {
                    val itemView =
                        recyclerView.findViewHolderForAdapterPosition(viewHolder.layoutPosition + i)?.itemView
                            ?: break
                    itemView.translationX = dX
                }
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder,
            target: ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
            val pos = viewHolder.adapterPosition
            val applet = applets[pos]
            if (applet is Flow) {
                viewModel.removeCandidate(applet)
                toast(R.string.format_applet_is_removed.format(applet.count))
            } else {
                val parent = applet.parent
                parent?.elements?.remove(applet)
                if (parent?.count == 0) {
                    // If all elements in this flow are removed, remove itself
                    viewModel.removeCandidate(parent)
                }
                toast(R.string.format_applet_is_removed.format(1))
            }
            viewModel.candidates.notifySelfChanged()
        }
    }

    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun setFlows(data: List<Flow>) {
        applets.clear()
        applets.addAll(
            data.flatMap {
                if (viewModel.isCollapsed(it)) {
                    Collections.singleton(it)
                } else {
                    Collections.singleton(it) + it.elements
                }
            }
        )
        var parent: Flow? = null
        var index = 0
        applets.forEach {
            if (it is Flow) {
                parent = it
                index = 0
            } else {
                it.parent = parent
                it.index = index++
            }
        }
    }

    fun updateFlows(data: List<Flow>) {
        val oldData = ArrayList(applets)
        setFlows(data)
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
                val new = applets[newItemPosition]
                if (new !is Flow && new.index == 0) {
                    return false
                }
                return oldItemPosition == 0 || newItemPosition != 0
            }
        }
        DiffUtil.calculateDiff(diffCallback).dispatchUpdatesTo(this)
    }

    inner class AppletViewHolder(val binding: ItemAppletCandidateBinding) :
        ViewHolder(binding.root) {
        init {
            binding.tvTitle.setOnClickListener {
                applets[adapterPosition].toggleRelation()
                notifyItemChanged(adapterPosition, true)
            }
            binding.ibAction.setOnClickListener {
                val applet = applets[adapterPosition]
                if (applet is Flow) {
                    val collapsed = viewModel.toggleCollapse(applet)
                    notifyItemChanged(adapterPosition, true)
                    setFlows(viewModel.candidates.require())
                    if (collapsed) {
                        notifyItemRangeRemoved(adapterPosition + 1, applet.count)
                    } else {
                        notifyItemRangeInserted(adapterPosition + 1, applet.count)
                    }
                    (recyclerView.parent as View).beginMyselfAutoTransition()
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

    private fun makeRelationSpan(origin: CharSequence, isAnd: Boolean): CharSequence {
        val relationText = if (isAnd) R.string.flow_and.text else R.string.flow_or.text
        return SpannableStringBuilder().append(
            relationText,
            ForegroundColorSpan(ColorSchemes.colorPrimary),
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
        holder.itemView.translationX = 0F
        holder.binding.let {
            val isFlow = applet is Flow
            val showRelation = position != 0 && applet.index != 0
            val option = viewModel.appletOptionFactory.findOption(applet)
            val title = option.getTitle(applet.isInverted)
            if (title != null && showRelation) {
                it.tvTitle.text = makeRelationSpan(title, applet.isAnd)
            } else {
                it.tvTitle.text = title
            }
            if (isFlow) {
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
                it.tvDesc.isVisible = applet !is PropertyCriterion<*>
                it.ibAction.isVisible = applet.isInvertible
                it.ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                it.tvTitle.setTextAppearance(TextAppearance_Material3_BodyMedium)
                if (applet is Criterion<*, *>) {
                    it.tvDesc.text = option.describe(applet.value)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return applets.size
    }
}