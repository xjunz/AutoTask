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
abstract class FlowItemTouchHelper(private val adapter: ListAdapter<Applet, *>) :
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

        override fun getChangePayload(oldItem: Applet, newItem: Applet): Any {
            return true
        }
    }

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
                if (currentList[vh.adapterPosition].isChildOf(applet)) {
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
        val to = target.adapterPosition
        val toApplet = currentList[target.adapterPosition]
        swapApplets(fromApplet, toApplet)
        fromApplet.requireParent().forEachIndexed { index, applet ->
            applet.index = index
        }
        if (fromApplet.index == 0 || toApplet.index == 0) {
            adapter.notifyItemChanged(from, true)
            adapter.notifyItemChanged(to, true)
        }
        notifyFlowChanged()
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val adapterPosition = viewHolder.adapterPosition
        if (adapterPosition == RecyclerView.NO_POSITION) return
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
                toast(R.string.format_applet_is_removed.format(from.size))
        } else {
            if (parent.size == 0)
                parent.parent?.remove(parent)
            toast(R.string.format_applet_is_removed.format(1))
        }
        parent.forEachIndexed { index, applet ->
            applet.index = index
        }
        adapter.notifyItemRangeChanged(pos - from.index, parent.size + 1, true)
        notifyFlowChanged()
    }
}