package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
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
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.ColorSchemes
import java.util.*

/**
 * @author xjunz 2022/10/03
 */
class AppletCandidatesAdapter(
    private val viewModel: FlowEditorViewModel,
    private val onClickListener: AppletOptionOnClickListener
) : RecyclerView.Adapter<AppletCandidatesAdapter.AppletViewHolder>() {

    private val applets = mutableListOf<Applet>()

    private var removedApplet: Applet? = null

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
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
                    val itemView = recyclerView.findViewHolderForAdapterPosition(
                        viewHolder.layoutPosition + i
                    )?.itemView ?: break
                    itemView.translationX = dX
                }
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder,
            target: ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            val fromApplet = applets[from]
            if (fromApplet is Flow) {
                val toFlow = applets[to] as Flow
                viewModel.swapFlows(fromApplet, toFlow)
                viewModel.candidates.notifySelfChanged()
            } else {
                val parent = fromApplet.parent!!
                Collections.swap(parent.elements, fromApplet.index, fromApplet.index + (to - from))
                parent.elements.forEachIndexed { index, applet ->
                    applet.index = index
                }
                Collections.swap(applets, from, to)
                notifyItemMoved(from, to)
            }
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val adapterPosition = viewHolder.adapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val target = applets[adapterPosition]
            if (target is Flow) return
            val parent = target.parent!!
            notifyItemRangeChanged(
                adapterPosition - target.index, parent.count, true
            )
        }

        override fun canDropOver(
            recyclerView: RecyclerView,
            current: ViewHolder,
            target: ViewHolder
        ): Boolean {
            val from = current.adapterPosition
            if (from == RecyclerView.NO_POSITION) return false
            val to = target.adapterPosition
            if (to == RecyclerView.NO_POSITION) return false
            if (applets[from].parent != applets[to].parent) return false
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
                if (parent?.count == 0)
                // If all elements in this flow are removed, remove itself
                    viewModel.removeCandidate(parent)

                toast(R.string.format_applet_is_removed.format(1))
            }
            removedApplet = applet
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
            override fun getOldListSize() = oldData.size

            override fun getNewListSize() = applets.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldData[oldItemPosition] === applets[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val new = applets[newItemPosition]
                if (new !is Flow && new.index == 0)
                    return false

                // Notify number changed
                if (new.parent == removedApplet?.parent)
                    return false

                return oldItemPosition == 0 || newItemPosition != 0
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val new = applets[newItemPosition]
                if (new.parent == removedApplet?.parent)
                    return true

                return null
            }
        }
        DiffUtil.calculateDiff(diffCallback).dispatchUpdatesTo(this)
        removedApplet = null
    }

    inner class AppletViewHolder(val binding: ItemAppletCandidateBinding) :
        ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val applet = applets[adapterPosition]
                if (applet is Flow) {
                    viewModel.toggleCollapse(applet)
                    viewModel.candidates.notifySelfChanged()
                } else {
                    onClickListener.onClick(applet) {
                        notifyItemChanged(adapterPosition, true)
                    }
                }
            }
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

    @SuppressLint("SetTextI18n")
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
        return applets.size
    }
}