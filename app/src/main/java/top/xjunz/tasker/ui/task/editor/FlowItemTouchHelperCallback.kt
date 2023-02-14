/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.graphics.Canvas
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.isDescendantOf
import java.util.*


/**
 * @author xjunz 2022/11/08
 */
open class FlowItemTouchHelperCallback(
    private val rv: RecyclerView,
    private val viewModel: FlowViewModel
) : SimpleCallback(UP or DOWN, LEFT or RIGHT) {

    private var undoSnackBar: Snackbar? = null

    init {
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                firstVisiblePosition = -1
                lastVisiblePosition = -1
            }
        })
        rv.findFragment<Fragment>().observe(viewModel.applets) {
            undoSnackBar?.dismiss()
            undoSnackBar = null
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Applet>() {
        override fun areItemsTheSame(oldItem: Applet, newItem: Applet): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Applet, newItem: Applet): Boolean {
            return true
        }
    }

    private val adapter by lazy {
        rv.adapter!!.casted<ListAdapter<Applet, *>>().apply {
            registerAdapterDataObserver(object : AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    firstVisiblePosition = -1
                    lastVisiblePosition = -1
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    firstVisiblePosition = -1
                    lastVisiblePosition = -1
                }
            })
        }
    }

    protected val Applet.isCollapsed: Boolean get() = viewModel.isCollapsed(this)

    protected val currentList: List<Applet> get() = adapter.currentList

    protected open fun onMoveEnded(hasDragged: Boolean, position: Int) {
        /* no-op */
    }

    protected open fun shouldBeInvolvedInSwipe(next: Applet, origin: Applet): Boolean {
        if (origin is Flow && !origin.isCollapsed) {
            return next.isDescendantOf(origin)
        }
        return false
    }

    protected open fun doRemove(parent: Flow, from: Applet): Set<Applet> {
        parent.remove(from)
        return Collections.singleton(from)
    }

    private val layoutManager: LinearLayoutManager by lazy {
        rv.layoutManager!!.casted()
    }

    private val pendingChangedApplets = mutableSetOf<Applet>()

    private var hasDragged: Boolean? = null
    private var hasSwapped: Boolean = false

    private var firstVisiblePosition = -1
    private var lastVisiblePosition = -1

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (viewHolder.adapterPosition == RecyclerView.NO_POSITION) return 0
        val applet = currentList[viewHolder.adapterPosition]
        if (applet.requiredIndex != -1) {
            return makeFlag(ACTION_STATE_DRAG, DOWN or UP) or makeFlag(ACTION_STATE_SWIPE, 0)
        }
        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (viewHolder == null) return
        if (actionState != ACTION_STATE_DRAG) return
        if (viewHolder.adapterPosition == RecyclerView.NO_POSITION) return
        val applet = currentList[viewHolder.adapterPosition]
        viewModel.onAppletLongClicked.value = applet
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
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return
        if (actionState == ACTION_STATE_DRAG) {
            if (dY != 0F || dX != 0F) {
                hasDragged = true
            } else if (hasDragged != true) {
                hasDragged = false
            }
        } else if (actionState == ACTION_STATE_SWIPE) {

            if (firstVisiblePosition == -1)
                firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            if (lastVisiblePosition == -1)
                lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

            val origin = currentList[position]
            for (i in firstVisiblePosition..lastVisiblePosition) {
                if (i == position) continue
                val vh = recyclerView.findViewHolderForAdapterPosition(i)
                if (vh != null) {
                    val next = currentList[vh.adapterPosition]
                    if (origin.requiredIndex == -1 && next.requiredIndex == -1
                        && shouldBeInvolvedInSwipe(next, origin)
                    ) {
                        vh.itemView.translationX = dX
                    }
                }
            }
        }
    }

    private inline val Applet.isSingle get() = this !is Flow || isEmpty()

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.adapterPosition
        val fromApplet = currentList[from]
        val to = target.adapterPosition
        val toApplet = currentList[to]
        Collections.swap(fromApplet.requireParent(), fromApplet.index, toApplet.index)
        hasSwapped = true
        if (fromApplet.index == 0 || toApplet.index == 0) {
            pendingChangedApplets.add(fromApplet)
            pendingChangedApplets.add(toApplet)
        }
        if (fromApplet.isSingle && toApplet.isSingle) {
            // If there is no flow with multiple children, do it more efficiently
            viewModel.regenerateApplets()
            adapter.notifyItemMoved(from, to)
        } else {
            viewModel.notifyFlowChanged()
        }
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        try {
            val adapterPosition = viewHolder.adapterPosition
            if (hasDragged != null)
                onMoveEnded(hasDragged!!, adapterPosition)
            if (adapterPosition == RecyclerView.NO_POSITION) return
            if (recyclerView.isComputingLayout) return
            if (hasSwapped) {
                // Update indexes
                val target = currentList[adapterPosition]
                if (target !is Flow || target.isContainer) {
                    val parent = target.requireParent()
                    adapter.notifyItemRangeChanged(
                        adapterPosition - target.index, parent.size, true
                    )
                }
                // Update relation text
                pendingChangedApplets.forEach {
                    notifyAppletChanged(it)
                }
            }
        } finally {
            hasDragged = null
            hasSwapped = false
            pendingChangedApplets.clear()
        }
    }

    // todo: Allow move applets within same scope
    private val Applet.scope: Flow?
        get() {
            var p = parent
            while (p != null && (p !is ScopeFlow<*> || p !is Do || p !is If)) {
                p = p.parent
            }
            return p
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
        val fromApplet = currentList[from]
        val toApplet = currentList[to]
        if (fromApplet.parent != currentList[to].parent) return false
        if (fromApplet is ControlFlow && toApplet is ControlFlow) return false
        return true
    }

    private fun findSnackBarParent(anchor: View): CoordinatorLayout {
        var p: View? = anchor
        while (p != null && p !is CoordinatorLayout) {
            p = p.parent as? View
        }
        return p!! as CoordinatorLayout
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        val from = currentList[pos]
        val parent = from.requireParent()
        val removed = doRemove(parent, from)
        val coordinatorLayout = findSnackBarParent(viewHolder.itemView)
        val snackBar = Snackbar.make(coordinatorLayout, R.string.removed, Snackbar.LENGTH_SHORT)
            .setAction(R.string.undo) {
                removed.sortedBy { it.index }.forEach {
                    if (it.index == parent.size) {
                        parent.add(it)
                    } else {
                        parent.add(it.index, it)
                    }
                }
                viewModel.notifyFlowChanged()
                viewModel.updateChildrenIndexesIfNeeded(parent)
            }
        val fab = coordinatorLayout.findViewById<View>(R.id.fab_action)
        if (fab != null && fab.isVisible) {
            snackBar.anchorView = fab
        }
        snackBar.show()
        if (from is Flow) {
            // Update relation text
            if (from.index == 0 && parent.isNotEmpty())
                notifyAppletChanged(parent.first())
        } else {
            // Update indexes
            viewModel.updateChildrenIndexesIfNeeded(parent)
        }
        if (parent.size == 0) {
            notifyAppletChanged(parent)
        }
        viewModel.notifyFlowChanged()
        undoSnackBar = snackBar
    }

    private fun notifyAppletChanged(applet: Applet) {
        adapter.notifyItemChanged(currentList.indexOf(applet))
    }
}