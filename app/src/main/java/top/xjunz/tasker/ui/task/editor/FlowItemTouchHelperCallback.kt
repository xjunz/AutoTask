package top.xjunz.tasker.ui.task.editor

import android.graphics.Canvas
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.toast
import java.util.*

/**
 * @author xjunz 2022/11/08
 */
abstract class FlowItemTouchHelperCallback(private val adapter: ListAdapter<Applet, *>) :
    ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {

    object DiffCallback : DiffUtil.ItemCallback<Applet>() {
        override fun areItemsTheSame(oldItem: Applet, newItem: Applet): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Applet, newItem: Applet): Boolean {
            return true
        }
    }

    private var swapFromApplet: Applet? = null
    private var swapToApplet: Applet? = null

    abstract val Flow.isCollapsed: Boolean

    protected open val currentList: List<Applet> get() = adapter.currentList

    abstract fun notifyFlowChanged()

    protected open fun swapApplets(from: Applet, to: Applet) {
        Collections.swap(from.requireParent(), from.index, to.index)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (viewHolder.adapterPosition == RecyclerView.NO_POSITION) return
        val applet = currentList[viewHolder.adapterPosition]
        if (applet is Flow && !applet.isCollapsed) {
            var i = 1
            var vh = recyclerView.findViewHolderForAdapterPosition(
                viewHolder.layoutPosition + 1
            )
            while (vh != null) {
                if (currentList[vh.adapterPosition].isDescendantOf(applet)) {
                    vh.itemView.translationX = dX
                    vh = recyclerView.findViewHolderForAdapterPosition(
                        viewHolder.layoutPosition + i++
                    )
                } else {
                    break
                }
            }
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.adapterPosition
        val fromApplet = currentList[from]
        val toApplet = currentList[target.adapterPosition]
        swapApplets(fromApplet, toApplet)
        fromApplet.requireParent().forEachIndexed { index, applet ->
            applet.index = index
        }
        if (fromApplet.index == 0 || toApplet.index == 0) {
            swapFromApplet = fromApplet
            swapToApplet = toApplet
        }
        notifyFlowChanged()
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val adapterPosition = viewHolder.adapterPosition
        if (adapterPosition == RecyclerView.NO_POSITION) return
        if (recyclerView.isComputingLayout) return
        if (swapFromApplet != null) {
            adapter.notifyItemChanged(currentList.indexOf(swapFromApplet))
            swapFromApplet = null
        }
        if (swapToApplet != null) {
            adapter.notifyItemChanged(currentList.indexOf(swapToApplet))
            swapToApplet = null
        }
        val target = currentList[adapterPosition]
        if (target is Flow) return
        val parent = target.requireParent()
        adapter.notifyItemRangeChanged(adapterPosition - target.index, parent.size, true)
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = current.adapterPosition
        if (from == RecyclerView.NO_POSITION) return false
        val to = target.adapterPosition
        if (to == RecyclerView.NO_POSITION) return false
        if (currentList[from] is ControlFlow) return false
        if (currentList[from].parent != currentList[to].parent) return false
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        val from = currentList[pos]
        val parent = from.requireParent()
        parent.remove(from)
        if (from is Flow) {
            if (from.isEmpty())
                toast(R.string.removed)
            else
                toast(R.string.format_applet_is_removed.format(from.flatSize))
        } else {
            if (parent.size == 0)
                parent.parent?.remove(parent)
            toast(R.string.format_applet_is_removed.format(1))
        }
        parent.forEachIndexed { index, applet ->
            applet.index = index
        }
        if (from is Flow) {
            if (from.index == 0 && parent.isNotEmpty())
                adapter.notifyItemChanged(currentList.indexOf(parent.first()))
        } else {
            adapter.notifyItemRangeChanged(pos - from.index, parent.size + 1, true)
        }
        notifyFlowChanged()
    }
}